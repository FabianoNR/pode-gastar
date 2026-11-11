# 0004: Front web em Vue + Vite, em repositório separado da API

Status: aceita

Data: 2026-11-11

## Contexto

O design aprovado prevê uma interface web responsiva para registrar o mês e consultar se uma compra cabe. Mobile aparece como próxima etapa. A API é o contrato que o front e o mobile vão consumir, então o front não deve depender de detalhes internos do backend.

## Decisão

- Front web responsivo em Vue + Vite.
- O front fica em repositório separado da API.
- Mobile fica para depois e consome a mesma API.
- O contrato da API é um artefato versionado no repositório da API, como um arquivo OpenAPI, e o front o usa como referência.

## Consequências

- Front e API evoluem com ciclos e deploys independentes, desde que o contrato seja respeitado.
- O mobile reaproveita o mesmo contrato, sem depender do código do front.

## Trade-offs

- O contrato precisa ser mantido como artefato explícito. Sem isso, o front e a API divergem sem aviso.
- Uma mudança que atravessa os dois repositórios exige PRs coordenadas, com ordem de merge definida.
- Dois repositórios aumentam o custo de setup e de CI em relação a um monorepo.
