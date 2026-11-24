#!/usr/bin/env bash
# Testes de retrospectiva-medir.sh, sem rede. Uso: .github/scripts/retrospectiva-medir-teste.sh
#
# Um gh falso vem primeiro no PATH: se o script tentar chamar o GitHub, o gh falso registra a
# chamada e o teste "nenhuma chamada ao gh" falha.
set -uo pipefail

DIRETORIO_DO_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$DIRETORIO_DO_SCRIPT/retrospectiva-medir.sh"
FIXTURES="$DIRETORIO_DO_SCRIPT/fixtures/retrospectiva"
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT

mkdir -p "$TEMP/bin"
cat >"$TEMP/bin/gh" <<FALSO
#!/usr/bin/env bash
echo "\$*" >>"$TEMP/chamadas-gh.txt"
exit 99
FALSO
chmod +x "$TEMP/bin/gh"
export PATH="$TEMP/bin:$PATH"

total=0
falhas=0

registrar() {
  local nome="$1" passou="$2" detalhe="${3:-}"
  total=$((total + 1))
  if [[ "$passou" == "sim" ]]; then
    echo "ok   - $nome"
  else
    falhas=$((falhas + 1))
    echo "FALHA - $nome $detalhe"
  fi
}

# medir <caso> <numero> [agora]: grava a saída em $TEMP/<caso>.json
medir() {
  local caso="$1" numero="$2" agora="${3:-2026-12-01T00:00:00Z}"
  RETROSPECTIVA_FIXTURES="$FIXTURES/$caso" RETROSPECTIVA_AGORA="$agora" \
    "$SCRIPT" "$numero" >"$TEMP/$caso.json" 2>"$TEMP/$caso.err"
}

# conferir <nome> <caso> <filtro jq> <valor esperado, em JSON compacto>
conferir() {
  local nome="$1" caso="$2" filtro="$3" esperado="$4" obtido
  obtido="$(jq -c "$filtro" "$TEMP/$caso.json" 2>&1)"
  if [[ "$obtido" == "$esperado" ]]; then
    registrar "$nome" sim
  else
    registrar "$nome" nao "(esperado $esperado, obtido $obtido)"
  fi
}

# --- Card #31: dados reais capturados do GitHub em 24/11/2026 ---
medir card-31 31
registrar "card #31: o script termina sem erro" "$([[ -s "$TEMP/card-31.json" ]] && echo sim || echo nao)" "$(cat "$TEMP/card-31.err")"
conferir "card #31: tempo em cada Status" card-31 '[.tempos[] | "\(.status)=\(.legivel)"]' \
  '["Backlog=2d4h39m02s","Entendimento=27m58s","Pronto p/ Concepção=23h44m25s","Concepção=1h04m54s","Pronto p/ Refinamento=23m57s","Refinamento=11h15m09s","Desenvolvimento=15m00s","Aguardando revisão=6m50s"]'
conferir "card #31: total da entrada em Entendimento até o Done" card-31 '.metricas.tempo_total_min' '2238.22'
conferir "card #31: fila e ativo somam o total" card-31 '.metricas | [.fila_total_min, .ativo_total_min, (.fila_total_min + .ativo_total_min | . * 100 | round / 100)]' '[1455.2,783.02,2238.22]'
conferir "card #31: sem voltas de Status" card-31 '.metricas.voltas_status' '0'
conferir "card #31: 7 de 7 itens do roteiro marcados" card-31 '[.roteiro.marcados, .roteiro.total, .metricas.roteiro_marcado_pct]' '[7,7,100]'
conferir "card #31: relatório do revisor sem pendência e com 4 sugestões" card-31 '[.metricas.pendencias_revisor, .metricas.sugestoes_revisor]' '[0,4]'
conferir "card #31: CI verde na primeira e PR sem evidência de QA" card-31 '[.pr, .metricas.ci_verde_primeira, .metricas.evidencia_qa]' '[57,"sim","nao"]'
conferir "card #31: 4 comentários de decisão, nenhum desvio, 3 edições do Refinamento" card-31 '[.metricas.comentarios_decisao, .metricas.desvios_roteiro, .metricas.edicoes_refinamento]' '[4,0,3]'
conferir "card #31: Situação Blocked/Hold fica vazia e listada como não encontrada" card-31 '[.metricas.vezes_blocked_hold, .nao_encontrado]' '[null,["vezes_blocked_hold"]]'
conferir "card #31: heurísticas marcadas como estimadas" card-31 '.estimado' '["desvios_roteiro","comentarios_decisao"]'
conferir "card #31: linha CSV" card-31 '.csv_linha' \
  '"31,\"Cartão de crédito reconhecido pelo marcador próprio, não pelo nome\",\"Dívida\",\"2026-11-24T14:38:58Z\",2238.22,1455.2,783.02,3159.03,27.97,1424.42,64.9,23.95,675.15,15,6.83,0,0,0,,0,4,\"sim\",\"nao\",100,0,3,4,,,,"'

# --- Card sintético com voltas, Hold e Em revisão ---
medir card-com-voltas 90
conferir "voltas: duas voltas de Status (Hold e volta à Concepção)" card-com-voltas '.metricas.voltas_status' '2'
conferir "voltas: Backlog soma antes e depois do Hold" card-com-voltas '.metricas.min_backlog' '2820'
conferir "voltas: total, fila (com o Backlog do Hold) e ativo" card-com-voltas '[.metricas.tempo_total_min, .metricas.fila_total_min, .metricas.ativo_total_min]' '[2160,1650,450]'
conferir "voltas: Em revisão fica fora de fila e ativo, sem coluna" card-com-voltas '[.fora_de_fila_e_ativo, (.tempos[] | select(.status == "Em revisão") | .min)]' '[["Em revisão"],60]'
conferir "voltas: eventos de outro projeto são ignorados" card-com-voltas '.metricas.min_entendimento' '90'
conferir "voltas: PR mesclada escolhida entre as que fecharam o card" card-com-voltas '.pr' '92'
conferir "voltas: último relatório do revisor, só itens de primeiro nível" card-com-voltas '[.metricas.pendencias_revisor, .metricas.sugestoes_revisor]' '[2,0]'
conferir "voltas: CI vermelho na primeira execução" card-com-voltas '.metricas.ci_verde_primeira' '"nao"'
conferir "voltas: evidência de QA com imagem" card-com-voltas '.metricas.evidencia_qa' '"sim"'
conferir "voltas: roteiro com 2 de 3 marcados (só o próprio bloco)" card-com-voltas '.metricas.roteiro_marcado_pct' '67'
conferir "voltas: um desvio e duas decisões pela heurística" card-com-voltas '[.metricas.desvios_roteiro, .metricas.comentarios_decisao]' '[1,2]'
conferir "voltas: sem Issue type e sem artefato Refinamento ficam não encontrados" card-com-voltas '.nao_encontrado' '["tipo","vezes_blocked_hold","edicoes_refinamento"]'
conferir "voltas: CSV com aspas escapadas e células vazias" card-com-voltas \
  '.csv_linha | startswith("90,\"Card \"\"com\"\" aspas, e vírgula\",,\"2026-01-03T12:00:00Z\",2160,")' 'true'

# --- Card sintético ainda em Desenvolvimento, sem PR ---
medir card-em-andamento 93 2026-02-01T17:00:00Z
conferir "andamento: último Status conta até o momento da medição" card-em-andamento '.metricas.min_desenvolvimento' '120'
conferir "andamento: sem Done, total, fila e ativo ficam vazios" card-em-andamento '[.metricas.data_done, .metricas.tempo_total_min, .metricas.fila_total_min, .metricas.ativo_total_min]' '[null,null,null,null]'
conferir "andamento: aviso de card fora de Done" card-em-andamento '.avisos' '["o card não está em Done; o último Status conta até medido_em"]'
conferir "andamento: sem PR, os campos da PR ficam não encontrados" card-em-andamento '.nao_encontrado' \
  '["data_done","tempo_total_min","fila_total_min","ativo_total_min","vezes_blocked_hold","pendencias_revisor","sugestoes_revisor","ci_verde_primeira","evidencia_qa","roteiro_marcado_pct"]'
conferir "andamento: artefato Refinamento sem edição" card-em-andamento '.metricas.edicoes_refinamento' '0'

# --- Uso, cabeçalho e proteção contra mutation ---
cabecalho="$("$SCRIPT" --cabecalho)"
registrar "cabeçalho com as 31 colunas, na ordem combinada" \
  "$([[ "$cabecalho" == id_card,titulo,tipo,data_done,tempo_total_min,*,achados_agente,nota_atrito && "$(tr ',' '\n' <<<"$cabecalho" | wc -l)" -eq 31 ]] && echo sim || echo nao)" "($cabecalho)"
registrar "cabeçalho igual ao do JSON" "$([[ "$cabecalho" == "$(jq -r .csv_cabecalho "$TEMP/card-31.json")" ]] && echo sim || echo nao)"
"$SCRIPT" >/dev/null 2>&1
registrar "sem argumento: erro de uso" "$([[ $? -eq 2 ]] && echo sim || echo nao)"
"$SCRIPT" abc >/dev/null 2>&1
registrar "número inválido: erro de uso" "$([[ $? -eq 2 ]] && echo sim || echo nao)"
(
  # shellcheck source=/dev/null
  source "$SCRIPT"
  consultar_graphql 'mutation { closeIssue(input: {issueId: "x"}) { clientMutationId } }' >/dev/null 2>&1
)
registrar "mutation é recusada antes de chamar o gh" "$([[ $? -eq 2 && ! -s "$TEMP/chamadas-gh.txt" ]] && echo sim || echo nao)"
(
  # shellcheck source=/dev/null
  source "$SCRIPT"
  consultar_graphql '  MUTATION{x}' >/dev/null 2>&1
)
registrar "mutation em maiúsculas também é recusada" "$([[ $? -eq 2 && ! -s "$TEMP/chamadas-gh.txt" ]] && echo sim || echo nao)"
registrar "as consultas do script são só query" \
  "$(source "$SCRIPT" && [[ "$CONSULTA_ISSUE" == query* && "$CONSULTA_PR" == query* ]] && echo sim || echo nao)"
registrar "nenhuma chamada ao gh nos testes" "$([[ ! -s "$TEMP/chamadas-gh.txt" ]] && echo sim || echo nao)" "($(cat "$TEMP/chamadas-gh.txt" 2>/dev/null))"

echo "$((total - falhas)) de $total passaram."
[[ $falhas -eq 0 ]]
