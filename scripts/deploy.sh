#!/usr/bin/env bash
# Deploy normal (docs/operacao.md, O4, O10, O15 e O16): confere os segredos, dispara o
# deploy hook do Render e espera o /health expor o commit publicado.
#
# Uso: deploy.sh <sha-publicado>
# Ambiente: RENDER_DEPLOY_HOOK_URL e SERVICE_URL (O14), obrigatórios (O15).

set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/operacao.sh"

executar_deploy_normal "$@"
