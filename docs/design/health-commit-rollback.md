# Design: `/health` com commit, deploy e rollback

Status: aprovado pelo Fabiano em 17/11/2026.

A spec correspondente é `docs/spec.md` (v8).

## Problema

Hoje o `/health` responde só `"UP"`. O smoke test e o rollback não conseguem dizer qual versão está no ar, e o rollback por imagem não garante a volta a um commit antigo, porque o serviço no Render é construído a partir do GitHub.

## Entendimento aprovado

As regras aprovadas estão em `docs/spec.md`, seção "Operação", e no CA13.

## Fora desta PR

Código, testes, workflows e README. Esta PR só altera documentos.
