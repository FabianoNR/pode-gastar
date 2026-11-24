#!/usr/bin/env bash
# Verifica a evidência de QA na descrição da PR.
#
# Uso: verificar-evidencia-qa.sh <arquivo-com-o-corpo-da-pr> <arquivo-com-os-caminhos-alterados>
#
# O arquivo de caminhos traz um caminho por linha (inclusive o nome antigo de arquivos renomeados).
# O texto-marcador vive só em .github/PULL_REQUEST_TEMPLATE.md: é a primeira linha de texto da
# seção "## Evidência de QA" do modelo. A variável MODELO_DA_PR troca o caminho do modelo (usada
# nos testes).
#
# Saída: 0 = passa (evidência ok ou não se aplica); 1 = evidência insuficiente; 2 = erro de uso.
set -euo pipefail

TITULO_DA_SECAO="## Evidência de QA"
DIRETORIO_DO_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODELO_DA_PR="${MODELO_DA_PR:-$DIRETORIO_DO_SCRIPT/../PULL_REQUEST_TEMPLATE.md}"

falhar_uso() {
  echo "Erro de uso: $1" >&2
  exit 2
}

# Imprime as linhas da seção "## Evidência de QA" (sem o título) de um texto em Markdown.
extrair_secao() {
  tr -d '\r' <"$1" | awk -v titulo="$TITULO_DA_SECAO" '
    emSecao && /^## / { exit }
    emSecao { print }
    { linha = $0; sub(/[ \t]+$/, "", linha) }
    linha == titulo { emSecao = 1 }
  '
}

tem_secao() {
  tr -d '\r' <"$1" | grep -qE "^${TITULO_DA_SECAO}[[:space:]]*$"
}

# Remove espaços nas pontas das linhas e as linhas em branco do começo.
aparar() {
  sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' | sed -e '/./,$!d'
}

caminho_e_isento() {
  case "$1" in
    docs/* | .claude/* | CLAUDE.md | README.md) return 0 ;;
    *) return 1 ;;
  esac
}

[[ $# -eq 2 ]] || falhar_uso "informe o arquivo do corpo da PR e o arquivo dos caminhos alterados."
ARQUIVO_DO_CORPO="$1"
ARQUIVO_DOS_CAMINHOS="$2"
[[ -f "$ARQUIVO_DO_CORPO" ]] || falhar_uso "arquivo do corpo da PR não encontrado: $ARQUIVO_DO_CORPO"
[[ -f "$ARQUIVO_DOS_CAMINHOS" ]] || falhar_uso "arquivo dos caminhos não encontrado: $ARQUIVO_DOS_CAMINHOS"
[[ -f "$MODELO_DA_PR" ]] || falhar_uso "modelo da PR não encontrado: $MODELO_DA_PR"

MARCADOR="$(extrair_secao "$MODELO_DA_PR" | aparar | head -n 1)"
[[ -n "$MARCADOR" ]] || falhar_uso "o modelo da PR não tem o texto-marcador sob '$TITULO_DA_SECAO'."

caminhos_lidos=0
exige_evidencia=0
while IFS= read -r caminho || [[ -n "$caminho" ]]; do
  caminho="${caminho%$'\r'}"
  [[ -n "$caminho" ]] || continue
  caminhos_lidos=$((caminhos_lidos + 1))
  if ! caminho_e_isento "$caminho"; then
    exige_evidencia=1
  fi
done <"$ARQUIVO_DOS_CAMINHOS"

[[ $caminhos_lidos -gt 0 ]] || falhar_uso "a lista de arquivos alterados está vazia."

if [[ $exige_evidencia -eq 0 ]]; then
  echo "Evidência de QA não se aplica: a PR altera só docs/**, .claude/**, CLAUDE.md e README.md."
  exit 0
fi

if ! tem_secao "$ARQUIVO_DO_CORPO"; then
  echo "FALHOU: a descrição da PR não tem a seção '$TITULO_DA_SECAO'. Esta PR altera código e precisa de evidência de QA." >&2
  exit 1
fi

# Comentários HTML de uma linha não contam como texto.
conteudo="$(extrair_secao "$ARQUIVO_DO_CORPO" | sed -e 's/<!--.*-->//g' | aparar)"

if [[ -z "$conteudo" ]]; then
  echo "FALHOU: a seção '$TITULO_DA_SECAO' está vazia. Preencha com o resumo do QA e pelo menos um print." >&2
  exit 1
fi

if printf '%s\n' "$conteudo" | grep -qF -- "$MARCADOR"; then
  echo "FALHOU: a seção '$TITULO_DA_SECAO' ainda tem o texto-marcador do modelo. Substitua-o pelo resumo do QA e por pelo menos um print." >&2
  exit 1
fi

if ! printf '%s\n' "$conteudo" | grep -qiE '!\[[^]]*\]\([^)]+\)|<img[[:space:]][^>]*>'; then
  echo "FALHOU: a seção '$TITULO_DA_SECAO' não tem imagem. Inclua pelo menos um print (![...](...) ou <img>)." >&2
  exit 1
fi

echo "Evidência de QA presente: a seção '$TITULO_DA_SECAO' está preenchida e tem pelo menos uma imagem."
