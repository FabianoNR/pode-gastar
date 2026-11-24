#!/usr/bin/env bash
# Mede um card do GitHub Project "Pode Gastar" para a retrospectiva. Só lê o GitHub.
#
# Uso:
#   retrospectiva-medir.sh <N>          imprime em stdout o JSON com as métricas do card <N>
#   retrospectiva-medir.sh --cabecalho  imprime só o cabeçalho do CSV
#
# O JSON traz as métricas, a linha CSV pronta (campo "csv_linha"), os campos que o GitHub não
# guarda ("nao_encontrado"), os contados por heurística ("estimado") e os que vêm da conversa
# ("da_conversa"). Nada é inventado nem estimado sem marcação: célula sem dado fica vazia.
#
# A chamada a gh api graphql é a exceção da ADR 0016 à ADR 0015: só query, nunca mutation.
#
# Fontes (todas só leitura):
#   - Issue, via GraphQL: título, Issue type, corpo (caixas do "Roteiro de desenvolvimento"),
#     comentários, eventos ProjectV2ItemStatusChangedEvent (createdAt, previousStatus, status),
#     ordem das opções do campo Status e PRs que fecharam o card.
#   - PR que fechou o card, via GraphQL: corpo, arquivos, comentários (relatório do revisor) e as
#     execuções do workflow "CI".
#   - Artefatos da issue: gh issue artifact list e view (versões do artefato "Refinamento").
#
# Regras de medição:
#   - Tempo em cada Status: do evento que entra no Status até o evento seguinte. O último Status,
#     se não for Done, conta até o momento da medição ("medido_em"). Minutos com duas casas.
#   - tempo_total: da primeira entrada em Entendimento até a última entrada em Done. O Backlog
#     antes do Entendimento fica só em min_backlog.
#   - Fila: Backlog, "Pronto p/ ..." e "Aguardando ...". Ativo: Entendimento, Concepção,
#     Refinamento, Desenvolvimento, Em revisão e Teste QA. Os dois somam só o que cai dentro do
#     tempo_total. Status fora das duas listas (hoje, nenhum) aparece em "tempos" e em
#     "fora_de_fila_e_ativo", sem coluna no CSV.
#   - voltas_status: mudanças para uma opção que vem antes da anterior na ordem do campo Status.
#   - vezes_blocked_hold: o histórico da Situação não aparece na linha do tempo; fica vazio.
#   - pendencias_revisor e sugestoes_revisor: último comentário da PR com as seções
#     "### Pendências" e "### Sugestões"; conta os itens de lista ("- ") de primeiro nível.
#   - ci_verde_primeira: a execução mais antiga do workflow "CI" entre os commits da PR terminou
#     com SUCCESS ("sim") ou não ("nao"). Se a execução foi refeita, vale a conclusão final dela.
#   - evidencia_qa: resultado de verificar-evidencia-qa.sh sobre o corpo e os arquivos da PR:
#     "sim", "nao" ou "nao_se_aplica".
#   - roteiro_marcado_pct: caixas marcadas sobre o total do bloco "## Roteiro de desenvolvimento".
#   - edicoes_refinamento: versões do artefato "Refinamento" menos a criação.
#   - Heurísticas (campo "estimado" no JSON):
#     - desvios_roteiro: comentários do card que contêm a palavra "desvio" (sem diferenciar
#       maiúsculas). Pega o comentário pedido pelo CLAUDE.md (passo 6), mas também qualquer
#       comentário que só cite a palavra.
#     - comentarios_decisao: comentários do card que contêm "Decisão:" e "Chapéu:" (o formato de
#       "Decisões durante o desenvolvimento" do CLAUDE.md e das skills), ignorando o negrito.
#
# Modo offline (testes): com RETROSPECTIVA_FIXTURES=<pasta>, lê issue.json, pr.json,
# artefatos.json e artefato-refinamento.json da pasta em vez de chamar o gh. Com
# RETROSPECTIVA_AGORA=<ISO 8601>, fixa o momento da medição.
#
# Saída: 0 = ok; 1 = card ou dado essencial não encontrado; 2 = erro de uso.
set -euo pipefail

DONO="FabianoNR"
REPOSITORIO="pode-gastar"
PROJETO="Pode Gastar"
DIRETORIO_DO_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERIFICADOR_DE_EVIDENCIA="$DIRETORIO_DO_SCRIPT/verificar-evidencia-qa.sh"

COLUNAS=(id_card titulo tipo data_done tempo_total_min fila_total_min ativo_total_min
  min_backlog min_entendimento min_pronto_concepcao min_concepcao min_pronto_refinamento
  min_refinamento min_desenvolvimento min_aguardando_revisao min_em_revisao min_aguardando_qa
  min_teste_qa
  voltas_status vezes_blocked_hold pendencias_revisor sugestoes_revisor ci_verde_primeira
  evidencia_qa roteiro_marcado_pct desvios_roteiro edicoes_refinamento comentarios_decisao
  achados_skill achados_fabiano achados_agente nota_atrito)

CONSULTA_ISSUE='query($dono: String!, $repositorio: String!, $numero: Int!) {
  repository(owner: $dono, name: $repositorio) {
    issue(number: $numero) {
      number
      title
      body
      issueType { name }
      comments(first: 100) { pageInfo { hasNextPage } nodes { body createdAt } }
      timelineItems(first: 200, itemTypes: [PROJECT_V2_ITEM_STATUS_CHANGED_EVENT]) {
        pageInfo { hasNextPage }
        nodes {
          ... on ProjectV2ItemStatusChangedEvent { createdAt previousStatus status project { title } }
        }
      }
      projectItems(first: 10) {
        nodes {
          project {
            title
            field(name: "Status") { ... on ProjectV2SingleSelectField { options { name } } }
          }
          fieldValueByName(name: "Status") { ... on ProjectV2ItemFieldSingleSelectValue { name } }
        }
      }
      closedByPullRequestsReferences(first: 10, includeClosedPrs: true) { nodes { number merged } }
    }
  }
}'

CONSULTA_PR='query($dono: String!, $repositorio: String!, $numero: Int!) {
  repository(owner: $dono, name: $repositorio) {
    pullRequest(number: $numero) {
      number
      body
      files(first: 100) { nodes { path } }
      comments(first: 100) { nodes { body createdAt } }
      commits(first: 100) {
        nodes {
          commit {
            checkSuites(first: 20) {
              nodes { conclusion workflowRun { createdAt workflow { name } } }
            }
          }
        }
      }
    }
  }
}'

falhar_uso() {
  echo "Erro de uso: $1" >&2
  exit 2
}

falhar() {
  echo "Erro: $1" >&2
  exit 1
}

# Executa uma consulta GraphQL. Só aceita query: qualquer mutation é recusada antes de chamar o gh.
consultar_graphql() {
  local consulta="$1"
  shift
  if printf '%s' "$consulta" | grep -qiE '(^|[^[:alnum:]_])mutation([^[:alnum:]_]|$)'; then
    echo "Erro: consulta recusada; este script só lê o GitHub (mutation não é permitida)." >&2
    return 2
  fi
  gh api graphql -f query="$consulta" "$@"
}

ler_issue() {
  local numero="$1"
  if [[ -n "${RETROSPECTIVA_FIXTURES:-}" ]]; then
    cat "$RETROSPECTIVA_FIXTURES/issue.json"
  else
    consultar_graphql "$CONSULTA_ISSUE" -f dono="$DONO" -f repositorio="$REPOSITORIO" -F numero="$numero"
  fi
}

ler_pr() {
  local numero="$1"
  if [[ -n "${RETROSPECTIVA_FIXTURES:-}" ]]; then
    cat "$RETROSPECTIVA_FIXTURES/pr.json"
  else
    consultar_graphql "$CONSULTA_PR" -f dono="$DONO" -f repositorio="$REPOSITORIO" -F numero="$numero"
  fi
}

ler_artefatos() {
  local numero="$1"
  if [[ -n "${RETROSPECTIVA_FIXTURES:-}" ]]; then
    cat "$RETROSPECTIVA_FIXTURES/artefatos.json"
  else
    gh issue artifact list "$numero" --repo "$DONO/$REPOSITORIO" --json number,name
  fi
}

# Imprime {versoes: <quantidade>} do artefato, sem o conteúdo.
ler_versoes_do_artefato() {
  local numero="$1" artefato="$2"
  if [[ -n "${RETROSPECTIVA_FIXTURES:-}" ]]; then
    cat "$RETROSPECTIVA_FIXTURES/artefato-refinamento.json"
  else
    gh issue artifact view "$numero" "$artefato" --repo "$DONO/$REPOSITORIO" --json versions \
      --jq '{versoes: (.versions | length)}'
  fi
}

# Imprime "sim", "nao" ou "nao_se_aplica", com o verificador do check evidencia-qa.
avaliar_evidencia_qa() {
  local pr_json="$1" temporario
  temporario="$(mktemp -d)"
  jq -r '.data.repository.pullRequest.body // ""' <<<"$pr_json" >"$temporario/corpo.md"
  jq -r '.data.repository.pullRequest.files.nodes[].path' <<<"$pr_json" >"$temporario/caminhos.txt"
  local saida codigo=0
  saida="$(MODELO_DA_PR="$DIRETORIO_DO_SCRIPT/../PULL_REQUEST_TEMPLATE.md" \
    "$VERIFICADOR_DE_EVIDENCIA" "$temporario/corpo.md" "$temporario/caminhos.txt" 2>&1)" || codigo=$?
  rm -rf "$temporario"
  case "$codigo" in
    0) if [[ "$saida" == *"não se aplica"* ]]; then echo "nao_se_aplica"; else echo "sim"; fi ;;
    1) echo "nao" ;;
    *) echo "" ;;
  esac
}

# Programa jq do cálculo. Cada def faz uma tarefa; o corpo no fim só junta as partes.
# Entrada: {issue, pr, agora, evidencia_qa, edicoes_refinamento}, montada em principal().
PROGRAMA_DE_CALCULO='
  def minutos(segundos): (segundos / 60 * 100 | round) / 100;
  def dois_digitos: "0\(.)" | .[-2:];

  # 2d4h39m02s, 1h04m54s, 15m00s.
  def legivel(segundos):
    (segundos | floor) as $total
    | ($total / 86400 | floor) as $dias
    | (($total % 86400) / 3600 | floor) as $horas
    | (($total % 3600) / 60 | floor) as $minutos
    | ($total % 60) as $segundos
    | if $dias > 0 or $horas > 0
      then (if $dias > 0 then "\($dias)d" else "" end) + "\($horas)h" + ($minutos | dois_digitos) + "m"
      else "\($minutos)m" end
      + ($segundos | dois_digitos) + "s";

  def instante: fromdateiso8601;
  def fila: . == "Backlog" or startswith("Pronto p/") or startswith("Aguardando");
  def ativo: IN("Entendimento", "Concepção", "Refinamento", "Desenvolvimento", "Em revisão", "Teste QA");
  def coluna_de: {
    "Backlog": "min_backlog", "Entendimento": "min_entendimento",
    "Pronto p/ Concepção": "min_pronto_concepcao", "Concepção": "min_concepcao",
    "Pronto p/ Refinamento": "min_pronto_refinamento", "Refinamento": "min_refinamento",
    "Desenvolvimento": "min_desenvolvimento", "Aguardando revisão": "min_aguardando_revisao",
    "Em revisão": "min_em_revisao", "Aguardando QA": "min_aguardando_qa", "Teste QA": "min_teste_qa" };
  def colunas_da_conversa: ["achados_skill", "achados_fabiano", "achados_agente", "nota_atrito"];

  # Eventos de Status do projeto, em ordem de data.
  def eventos_de_status(projeto):
    .timelineItems.nodes
    | map(select(.project.title == projeto and .status != null))
    | sort_by(.createdAt);

  # Um intervalo por passagem num Status (menos Done): {status, inicio, fim}, em segundos.
  def intervalos(agora):
    . as $eventos
    | [ range(0; $eventos | length) as $i
        | { status: $eventos[$i].status,
            inicio: ($eventos[$i].createdAt | instante),
            fim: (($eventos[$i + 1].createdAt // agora) | instante) } ]
    | map(select(.status != "Done"));

  # Segundos dos intervalos cujo Status passa no filtro, recortados pela janela [inicio, fim].
  def segundos_na_janela(janela; filtro):
    if janela == null then null
    else [ .[] | select(.status | filtro)
           | ([.fim, janela[1]] | min) - ([.inicio, janela[0]] | max)
           | select(. > 0) ] | add // 0 end;

  def segundos_por_status:
    group_by(.status) | map({ key: .[0].status, value: (map(.fim - .inicio) | add) }) | from_entries;

  def voltas(ordem):
    . as $eventos
    | [ range(1; $eventos | length) as $i
        | (ordem | index($eventos[$i].previousStatus)) as $posicao_anterior
        | (ordem | index($eventos[$i].status)) as $posicao_nova
        | select($posicao_anterior != null and $posicao_nova != null and $posicao_nova < $posicao_anterior) ]
    | length;

  # Caixas do bloco "## Roteiro de desenvolvimento" do corpo: {total, marcados} ou null.
  def roteiro:
    gsub("\r"; "") | split("\n")
    | (map(test("^## Roteiro de desenvolvimento\\s*$")) | index(true)) as $inicio
    | if $inicio == null then null
      else .[$inicio + 1:]
        | (map(test("^## ")) | index(true)) as $fim
        | (if $fim == null then . else .[:$fim] end)
        | { total: map(select(test("^\\s*- \\[[ xX]\\]"))) | length,
            marcados: map(select(test("^\\s*- \\[[xX]\\]"))) | length } end;

  # Último comentário com as seções "### Pendências" e "### Sugestões".
  def relatorio_do_revisor:
    map(.body | gsub("\r"; ""))
    | map(select(test("(^|\\n)### Pend[eê]ncias") and test("(^|\\n)### Sugest[oõ]es")))
    | last;

  # Itens de lista de primeiro nível ("- ") dentro da seção "### <secao>".
  def contar_itens(secao):
    [ split("\n")
      | foreach .[] as $linha ({ dentro: false, item: false };
          if ($linha | test("^#{2,3} ")) then { dentro: ($linha | test("^### " + secao)), item: false }
          else { dentro: .dentro, item: (.dentro and ($linha | test("^- "))) } end;
          .item)
      | select(.) ]
    | length;

  # A execução mais antiga do workflow "CI" entre os commits da PR.
  def primeiro_ci:
    [ .commits.nodes[]?.commit.checkSuites.nodes[] | select(.workflowRun.workflow.name == "CI") ]
    | sort_by(.workflowRun.createdAt) | first;

  def celula_csv:
    if . == null then ""
    elif type == "string" then "\"" + gsub("\""; "\"\"") + "\""
    else tostring end;

  def ou_nulo(filtro): if . == null then null else filtro end;

  .issue.data.repository.issue as $issue
  | .agora as $agora
  | (.pr.data.repository.pullRequest // null) as $pr
  | ($issue.projectItems.nodes | map(select(.project.title == $projeto)) | first) as $item_do_projeto
  | ($item_do_projeto.project.field.options // [] | map(.name)) as $ordem
  | ($issue | eventos_de_status($projeto)) as $eventos
  | ($eventos | intervalos($agora)) as $intervalos
  | ($intervalos | segundos_por_status) as $segundos
  | ($eventos | map(select(.status == "Entendimento")) | first | .createdAt) as $entrada_entendimento
  | ($eventos | map(select(.status == "Done")) | last | .createdAt) as $entrada_done
  | (if $entrada_entendimento and $entrada_done
     then [($entrada_entendimento | instante), ($entrada_done | instante)] else null end) as $janela
  | ($issue.body // "" | roteiro) as $roteiro
  | ($pr.comments.nodes // [] | relatorio_do_revisor) as $relatorio
  | ($pr | primeiro_ci) as $execucao_ci
  | ($issue.comments.nodes | map(.body)) as $comentarios
  | ($item_do_projeto.fieldValueByName.name // null) as $status_atual
  | {
      id_card: $issue.number,
      titulo: $issue.title,
      tipo: ($issue.issueType.name // null),
      data_done: $entrada_done,
      tempo_total_min: ($janela | ou_nulo(minutos(.[1] - .[0]))),
      fila_total_min: ($intervalos | segundos_na_janela($janela; fila) | ou_nulo(minutos(.))),
      ativo_total_min: ($intervalos | segundos_na_janela($janela; ativo) | ou_nulo(minutos(.))),
      voltas_status: (if ($ordem | length) > 0 then $eventos | voltas($ordem) else null end),
      vezes_blocked_hold: null,
      pendencias_revisor: ($relatorio | ou_nulo(contar_itens("Pend"))),
      sugestoes_revisor: ($relatorio | ou_nulo(contar_itens("Sugest"))),
      ci_verde_primeira: ($execucao_ci | ou_nulo(if .conclusion == "SUCCESS" then "sim" else "nao" end)),
      evidencia_qa: (if .evidencia_qa == "" then null else .evidencia_qa end),
      roteiro_marcado_pct: (if $roteiro and $roteiro.total > 0 then ($roteiro.marcados * 100 / $roteiro.total | round) else null end),
      desvios_roteiro: ($comentarios | map(select(test("desvio"; "i"))) | length),
      edicoes_refinamento: .edicoes_refinamento,
      comentarios_decisao: ($comentarios | map(gsub("\\*"; "")) | map(select(test("Decisão:") and test("Chapéu:"))) | length)
    }
    + (colunas_da_conversa | map({ key: ., value: null }) | from_entries)
    + (coluna_de | to_entries | map({ key: .value, value: minutos($segundos[.key] // 0) }) | from_entries)
  | . as $metricas
  | {
      card: $metricas.id_card,
      status_atual: $status_atual,
      pr: ($pr.number // null),
      medido_em: $agora,
      tempos: ($segundos | to_entries | sort_by(.key as $status | $ordem | index($status))
               | map({ status: .key, min: minutos(.value), legivel: legivel(.value) })),
      fora_de_fila_e_ativo: ($segundos | keys | map(select((fila or ativo) | not))),
      roteiro: $roteiro,
      artefatos: $artefatos,
      metricas: ([ $colunas[] as $coluna | { key: $coluna, value: $metricas[$coluna] } ] | from_entries),
      estimado: ["desvios_roteiro", "comentarios_decisao"],
      da_conversa: colunas_da_conversa,
      nao_encontrado: [ $colunas[] as $coluna
        | select($metricas[$coluna] == null and (colunas_da_conversa | index($coluna) | not))
        | $coluna ],
      avisos: [ (if $issue.comments.pageInfo.hasNextPage then "o card tem mais de 100 comentários; só os 100 primeiros foram lidos" else empty end),
                (if $issue.timelineItems.pageInfo.hasNextPage then "a linha do tempo tem mais de 200 eventos; só os 200 primeiros foram lidos" else empty end),
                (if $status_atual != "Done" then "o card não está em Done; o último Status conta até medido_em" else empty end) ],
      csv_cabecalho: ($colunas | join(",")),
      csv_linha: ([ $colunas[] as $coluna | $metricas[$coluna] | celula_csv ] | join(","))
    }
'

# Recebe a lista de nomes dos artefatos e o JSON de entrada; imprime o JSON de saída.
calcular() {
  local artefatos="$1" entrada="$2" colunas_json
  colunas_json="$(printf '%s\n' "${COLUNAS[@]}" | jq -R . | jq -s .)"
  jq --argjson colunas "$colunas_json" --arg projeto "$PROJETO" --argjson artefatos "$artefatos" \
    "$PROGRAMA_DE_CALCULO" <<<"$entrada"
}

principal() {
  [[ $# -eq 1 ]] || falhar_uso "informe o número do card (ou --cabecalho)."
  if [[ "$1" == "--cabecalho" ]]; then
    (IFS=,; echo "${COLUNAS[*]}")
    return 0
  fi
  [[ "$1" =~ ^[0-9]+$ ]] || falhar_uso "o número do card deve ser inteiro: $1"
  local numero="$1"
  local agora="${RETROSPECTIVA_AGORA:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"

  local issue_json
  issue_json="$(ler_issue "$numero")"
  jq -e '.data.repository.issue.number' <<<"$issue_json" >/dev/null || falhar "card #$numero não encontrado."

  local numero_da_pr pr_json="null" evidencia=""
  numero_da_pr="$(jq -r '.data.repository.issue.closedByPullRequestsReferences.nodes
    | ((map(select(.merged)) | last) // last) | .number // empty' <<<"$issue_json")"
  if [[ -n "$numero_da_pr" ]]; then
    pr_json="$(ler_pr "$numero_da_pr")"
    evidencia="$(avaliar_evidencia_qa "$pr_json")"
  fi

  local artefatos edicoes="null" numero_do_refinamento
  artefatos="$(ler_artefatos "$numero")"
  numero_do_refinamento="$(jq -r 'map(select(.name == "Refinamento")) | first.number // empty' <<<"$artefatos")"
  if [[ -n "$numero_do_refinamento" ]]; then
    edicoes="$(ler_versoes_do_artefato "$numero" "$numero_do_refinamento" | jq '.versoes - 1')"
  fi

  local entrada
  entrada="$(jq -n --argjson issue "$issue_json" --argjson pr "$pr_json" --arg agora "$agora" \
    --arg evidencia "$evidencia" --argjson edicoes "$edicoes" \
    '{issue: $issue, pr: $pr, agora: $agora, evidencia_qa: $evidencia, edicoes_refinamento: $edicoes}')"
  calcular "$(jq 'map(.name)' <<<"$artefatos")" "$entrada"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  principal "$@"
fi
