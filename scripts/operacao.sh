#!/usr/bin/env bash
# Funções do deploy normal e do rollback (docs/operacao.md, O4 a O16).
# Carregada por deploy.sh e rollback.sh; os testes automatizados chamam as funções direto.
#
# Intervalo e prazo da espera pelo commit (O13): 15 s e 10 min. Tempo limite de cada
# chamada à API do Render e ao deploy hook (O16): 30 s. As variáveis
# ESPERA_INTERVALO_SEGUNDOS, ESPERA_PRAZO_SEGUNDOS e LIMITE_DA_CHAMADA_SEGUNDOS existem
# só para os testes automatizados; nenhum workflow as define.

set -euo pipefail

readonly INTERVALO_PADRAO_EM_SEGUNDOS=15
readonly PRAZO_PADRAO_EM_SEGUNDOS=600
readonly LIMITE_DA_CONSULTA_EM_SEGUNDOS=5
readonly LIMITE_PADRAO_DA_CHAMADA_EM_SEGUNDOS=30
readonly SAIDA_DO_CURL_POR_TEMPO_LIMITE=28
readonly COMMIT_DESCONHECIDO="desconhecido"
readonly SEM_RESPOSTA="000"

falhar() {
  echo "::error::$1" >&2
  exit 1
}

agora_em_milissegundos() {
  date +%s%3N
}

e_resposta_2xx() {
  [[ "$1" =~ ^2[0-9][0-9]$ ]]
}

# O15: falha antes de qualquer chamada HTTP se algum segredo estiver ausente, vazio ou só
# com espaços. A mensagem traz só os nomes, nunca os valores.
exigir_segredos() {
  local segredo valor ausentes=()
  for segredo in "$@"; do
    valor="${!segredo:-}"
    if [ -z "${valor//[[:space:]]/}" ]; then
      ausentes+=("$segredo")
    fi
  done
  if [ "${#ausentes[@]}" -gt 0 ]; then
    falhar "Segredo obrigatório ausente ou vazio: ${ausentes[*]}. Nenhuma chamada foi feita."
  fi
}

# O6: exatamente 40 caracteres hexadecimais minúsculos.
validar_sha() {
  local sha="$1"
  if ! [[ "$sha" =~ ^[0-9a-f]{40}$ ]]; then
    falhar "SHA inválido ('${sha}'): o rollback exige exatamente 40 caracteres hexadecimais minúsculos."
  fi
}

limite_da_chamada_em_segundos() {
  echo "${LIMITE_DA_CHAMADA_SEGUNDOS:-$LIMITE_PADRAO_DA_CHAMADA_EM_SEGUNDOS}"
}

# O4, O9, O16: classifica o resultado de uma chamada única (deploy hook ou API do Render).
# Falha na hora se a chamada passou do tempo limite, ficou sem resposta final ou não teve 2xx.
exigir_resposta_2xx() {
  local chamada="$1" fluxo="$2" saida_do_curl="$3" codigo="$4"
  if [ "$saida_do_curl" -eq "$SAIDA_DO_CURL_POR_TEMPO_LIMITE" ]; then
    falhar "A chamada ${chamada} passou do tempo limite de $(limite_da_chamada_em_segundos) s. ${fluxo} encerrado sem nova tentativa e sem consultar o /health."
  fi
  if [ "$codigo" = "$SEM_RESPOSTA" ] || [[ "$codigo" =~ ^1 ]]; then
    falhar "A chamada ${chamada} não teve resposta. ${fluxo} encerrado sem nova tentativa e sem consultar o /health."
  fi
  if ! e_resposta_2xx "$codigo"; then
    falhar "A chamada ${chamada} respondeu HTTP ${codigo}. ${fluxo} encerrado sem consultar o /health."
  fi
}

# O5, O8, O9, O16: uma única chamada, sem seguir redirecionamento. Só 2xx aceita o pedido.
pedir_deploy_do_commit() {
  local url_da_api="$1" sha="$2" codigo saida_do_curl=0
  codigo=$(
    curl --silent --output /dev/null --write-out '%{http_code}' \
      --max-time "$(limite_da_chamada_em_segundos)" \
      --request POST \
      --header 'Accept: application/json' \
      --header 'Content-Type: application/json' \
      --config <(printf 'header = "Authorization: Bearer %s"\n' "$RENDER_API_KEY") \
      --data "{\"commitId\": \"${sha}\"}" \
      "${url_da_api}/v1/services/${RENDER_SERVICE_ID}/deploys"
  ) || saida_do_curl=$?
  exigir_resposta_2xx "à API do Render" "Rollback" "$saida_do_curl" "$codigo"
  echo "A API do Render aceitou o pedido de deploy do commit ${sha} (HTTP ${codigo})."
}

# O4, O16: uma única chamada, sem seguir redirecionamento. Só 2xx leva à espera pelo
# /health. O Render constrói o último commit da main (ADR 0014), sem parâmetro extra.
disparar_deploy_hook() {
  local url_do_hook="$1" codigo saida_do_curl=0
  codigo=$(
    curl --silent --output /dev/null --write-out '%{http_code}' \
      --max-time "$(limite_da_chamada_em_segundos)" \
      --request POST \
      "$url_do_hook"
  ) || saida_do_curl=$?
  exigir_resposta_2xx "ao deploy hook" "Job" "$saida_do_curl" "$codigo"
  echo "O deploy hook aceitou o pedido de deploy (HTTP ${codigo})."
}

# O10: uma consulta ao /health. Retorna 0 se o commit exposto é o esperado; senão imprime
# o motivo do "ainda não" e retorna 1.
consultar_commit() {
  local url_do_servico="$1" sha_esperado="$2" arquivo_do_corpo codigo saida_do_curl commit
  arquivo_do_corpo=$(mktemp)
  saida_do_curl=0
  codigo=$(
    curl --silent --max-time "$LIMITE_DA_CONSULTA_EM_SEGUNDOS" \
      --output "$arquivo_do_corpo" --write-out '%{http_code}' \
      "${url_do_servico}/health"
  ) || saida_do_curl=$?
  commit=$(
    jq --raw-output \
      'if type == "object" and (.commit | type) == "string" then .commit else "" end' \
      "$arquivo_do_corpo" 2> /dev/null
  ) || commit=""
  rm -f "$arquivo_do_corpo"

  if [ "$saida_do_curl" -ne 0 ]; then
    echo "sem resposta: erro ${saida_do_curl} do curl"
  elif [ "$codigo" != "200" ]; then
    echo "HTTP ${codigo}"
  elif [ -z "$commit" ]; then
    echo "resposta sem um campo commit legível"
  elif [ "$commit" = "$COMMIT_DESCONHECIDO" ]; then
    echo "commit ${COMMIT_DESCONHECIDO}"
  elif [ "$commit" != "$sha_esperado" ]; then
    echo "commit ${commit} no ar"
  else
    return 0
  fi
  return 1
}

# O7, O10, O13: consulta até o commit esperado aparecer. O prazo começa na chamada desta
# função, logo depois da resposta 2xx. Nenhuma consulta começa depois do fim do prazo.
esperar_commit() {
  local url_do_servico="$1" sha_esperado="$2"
  local intervalo="${ESPERA_INTERVALO_SEGUNDOS:-$INTERVALO_PADRAO_EM_SEGUNDOS}"
  local prazo="${ESPERA_PRAZO_SEGUNDOS:-$PRAZO_PADRAO_EM_SEGUNDOS}"
  local fim_do_prazo numero_da_consulta=1 motivo
  fim_do_prazo=$(($(agora_em_milissegundos) + prazo * 1000))

  while true; do
    if motivo=$(consultar_commit "$url_do_servico" "$sha_esperado"); then
      echo "Consulta ${numero_da_consulta} ao /health: commit ${sha_esperado} no ar."
      return 0
    fi
    echo "Consulta ${numero_da_consulta} ao /health: ainda não (${motivo})."
    sleep "$intervalo"
    if (($(agora_em_milissegundos) >= fim_do_prazo)); then
      falhar "Prazo de ${prazo} s esgotado: o /health não expôs o commit ${sha_esperado}."
    fi
    numero_da_consulta=$((numero_da_consulta + 1))
  done
}

# O5 a O10, O15, O16. Usa RENDER_API_KEY, RENDER_SERVICE_ID e SERVICE_URL (O14).
# A checagem dos segredos vem antes da validação do SHA (O15).
executar_rollback() {
  local url_da_api="$1" sha="${2:-}"
  exigir_segredos RENDER_API_KEY RENDER_SERVICE_ID SERVICE_URL
  validar_sha "$sha"
  pedir_deploy_do_commit "$url_da_api" "$sha"
  esperar_commit "$SERVICE_URL" "$sha"
}

# O4, O10, O15, O16. Usa RENDER_DEPLOY_HOOK_URL e SERVICE_URL (O14).
executar_deploy_normal() {
  local sha_publicado="${1:-}"
  exigir_segredos RENDER_DEPLOY_HOOK_URL SERVICE_URL
  disparar_deploy_hook "$RENDER_DEPLOY_HOOK_URL"
  esperar_commit "$SERVICE_URL" "$sha_publicado"
}
