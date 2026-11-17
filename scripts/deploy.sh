#!/usr/bin/env bash
# Deploy normal (docs/spec.md, O4 e O10): dispara o deploy hook do Render e espera o
# /health expor o commit publicado.
#
# Uso: deploy.sh <sha-publicado> <imagem>
# Ambiente: RENDER_DEPLOY_HOOK_URL e SERVICE_URL (O14).

set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/operacao.sh"

executar_deploy_normal "$@"
