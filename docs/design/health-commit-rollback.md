# Design: `/health` com commit, deploy e rollback

Status: aprovado pelo Fabiano em 17/11/2026.

A spec correspondente é `docs/spec.md` (v8, seção "Operação", e CA13). As regras ficam só na spec; este resumo registra o que foi decidido e onde.

## Problema

Hoje o `/health` responde só `"UP"`. O smoke test e o rollback não conseguem dizer qual versão está no ar, e o rollback por imagem não garante a volta a um commit antigo, porque o serviço no Render é construído a partir do GitHub.

## Entendimento aprovado

- O `/health` passa a informar o commit no ar, com um valor fixo quando roda localmente (spec O1 a O3; CA13).
- O deploy normal só é confirmado quando o `/health` mostra o commit publicado (spec O4).
- O rollback usa a API do Render com o commit desejado e só é confirmado pelo `/health` (spec O5 a O7; ADR 0013).
- Se a publicação no GHCR ainda é necessária fica para outra PR (spec, "Abertos").

## Fora desta PR

Código, testes, workflows e README. Esta PR só altera documentos.
