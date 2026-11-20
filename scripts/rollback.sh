#!/usr/bin/env bash
# Rollback pela API do Render (docs/operacao.md, O5 a O10, O15 e O16): confere os segredos,
# valida o SHA, pede o deploy desse commit e espera o /health expor o commit.
#
# Uso: rollback.sh <sha>
# Ambiente: RENDER_API_KEY, RENDER_SERVICE_ID e SERVICE_URL (O14), obrigatórios (O15).
#
# O endereço da API é fixo: nenhuma variável de ambiente o troca.

set -euo pipefail

readonly URL_DA_API_DO_RENDER="https://api.render.com"

source "$(dirname "${BASH_SOURCE[0]}")/operacao.sh"

executar_rollback "$URL_DA_API_DO_RENDER" "$@"
