# 0013: Rollback pela API do Render com `commitId`

Status: aceita

Data: 2026-11-17

## Contexto

A ADR 0009 hospeda a API no Render e deixou o procedimento de rollback para depois. O serviço no Render é construído a partir do repositório no GitHub e não puxa imagem do GHCR. Por isso, apontar o deploy para uma imagem antiga não garante a volta a um commit antigo.

## Decisão

O rollback pede ao Render, pela API, um deploy do commit desejado (`commitId`) e só é considerado concluído quando o `/health` expõe esse commit. As regras estão em `docs/spec.md`, seção "Operação" (O1 a O7).

## Consequências

- O rollback passa a depender da chave da API do Render e do id do serviço. Onde eles ficam será definido na PR que implementar o workflow.
- O `/health` passa a informar qual commit está no ar, e o mesmo critério confirma o deploy normal.
- A necessidade da publicação no GHCR fica em aberto (spec, "Abertos").
