#!/usr/bin/env bash
# Testes de verificar-evidencia-qa.sh. Uso: .github/scripts/verificar-evidencia-qa-teste.sh
set -uo pipefail

DIRETORIO_DO_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$DIRETORIO_DO_SCRIPT/verificar-evidencia-qa.sh"
MODELO="$DIRETORIO_DO_SCRIPT/../PULL_REQUEST_TEMPLATE.md"
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT

MARCADOR="$(awk '/^## Evidência de QA/ {s=1; next} s && NF {print; exit}' "$MODELO")"
total=0
falhas=0

# executar <nome> <codigo-esperado> <trecho-esperado-na-saida> <corpo> <caminhos...>
executar() {
  local nome="$1" esperado="$2" trecho="$3" corpo="$4"
  shift 4
  printf '%s' "$corpo" >"$TEMP/corpo.md"
  printf '%s\n' "$@" >"$TEMP/caminhos.txt"
  local saida codigo
  saida="$("$SCRIPT" "$TEMP/corpo.md" "$TEMP/caminhos.txt" 2>&1)"
  codigo=$?
  total=$((total + 1))
  if [[ $codigo -eq $esperado && "$saida" == *"$trecho"* ]]; then
    echo "ok   - $nome"
  else
    falhas=$((falhas + 1))
    echo "FALHA - $nome (código $codigo, esperado $esperado; saída: $saida)"
  fi
}

CODIGO="src/main/java/Exemplo.java"
CORPO_BASE=$'## O que é\n\nalgo\n\n## Riscos conhecidos\n\nnenhum\n'

executar "sem a seção" 1 "não tem a seção" "$CORPO_BASE" "$CODIGO"
executar "seção vazia" 1 "está vazia" "$CORPO_BASE"$'\n## Evidência de QA\n\n\n' "$CODIGO"
executar "seção só com o marcador" 1 "texto-marcador" "$CORPO_BASE"$'\n## Evidência de QA\n\n'"$MARCADOR"$'\n' "$CODIGO"
executar "texto sem imagem" 1 "não tem imagem" "$CORPO_BASE"$'\n## Evidência de QA\n\nTestei no Postman, tudo certo.\n' "$CODIGO"
executar "texto com imagem em Markdown" 0 "Evidência de QA presente" "$CORPO_BASE"$'\n## Evidência de QA\n\nPOST /decisoes deu 200.\n\n![print](https://exemplo.com/a.png)\n' "$CODIGO"
executar "texto com tag img" 0 "Evidência de QA presente" "$CORPO_BASE"$'\n## Evidência de QA\n\nPOST /decisoes deu 200.\n\n<img width="300" src="https://exemplo.com/a.png">\n' "$CODIGO"
executar "PR só de documentação é isenta" 0 "não se aplica" "$CORPO_BASE" "docs/spec.md" "CLAUDE.md" ".claude/skills/x/SKILL.md" "README.md"
executar "PR com código e evidência completa" 0 "Evidência de QA presente" "$CORPO_BASE"$'\n## Evidência de QA\n\nResumo.\n![print](https://exemplo.com/a.png)\n' "docs/spec.md" "$CODIGO"
executar "marcador mais imagem ainda falha" 1 "texto-marcador" "$CORPO_BASE"$'\n## Evidência de QA\n\n'"$MARCADOR"$'\n![p](https://x.com/a.png)\n' "$CODIGO"
executar "imagem fora da seção não vale" 1 "não tem imagem" $'## O que é\n\n![p](https://x.com/a.png)\n\n## Evidência de QA\n\nsó texto\n\n## Outra\n' "$CODIGO"
executar "workflow em .github não é isento" 1 "não tem a seção" "$CORPO_BASE" ".github/workflows/ci.yml"
executar "corpo com quebra de linha CRLF e imagem passa" 0 "Evidência de QA presente" $'## Evidência de QA\r\n\r\nok\r\n![p](https://x.com/a.png)\r\n' "$CODIGO"

echo "$((total - falhas)) de $total passaram."
[[ $falhas -eq 0 ]]
