# 0013: Rollback pela API do Render com `commitId`

Status: aceita

Data: 2026-11-17

## Contexto

A ADR 0009 hospeda a API no Render e deixou o procedimento de rollback para depois. O serviço no Render é construído a partir do repositório no GitHub e não puxa imagem do GHCR. Por isso, apontar o deploy para uma imagem antiga não garante a volta a um commit antigo.

## Decisão

A regra do rollback é a de `docs/spec.md`, seção "Operação".

## Consequências

- O rollback passa a depender da chave da API do Render e do id do serviço. Os dois ficam nos segredos do repositório no GitHub (spec, O14), com os nomes `RENDER_API_KEY` (chave da API) e `RENDER_SERVICE_ID` (id do serviço).
- A necessidade da publicação no GHCR fica em aberto (spec, "Abertos").
