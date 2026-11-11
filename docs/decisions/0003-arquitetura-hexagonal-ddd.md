# 0003: Arquitetura hexagonal com DDD

Status: aceita

Data: 2026-11-11

## Contexto

O design aprovado (`docs/design/design-pode-gastar.md`) tem regras de domínio que não são um CRUD: custo fixo, investimento, orçamento variável, status de itens (PENDENTE, EM_DISCUSSAO, PARCIAL, REALIZADO), dois cálculos (total restante e livre) e sugestões de corte em ordem de prioridade. Essas regras precisam ser testadas sem subir a aplicação. A mesma lógica também deve servir à API web e, depois, a um app mobile.

## Decisão

- O núcleo de domínio não conhece Spring nem JSON. Não há anotação de framework nem tipo de serialização dentro do domínio.
- A porta de entrada é o agregado `Mes`, lido de uma porta de saída. Nesta fase, essa porta é mockada em memória.
- Os adaptadores de entrada (web, e depois mobile) traduzem DTOs para o domínio e de volta.
- Banco de dados e repositório real entram depois, como novo adaptador de saída. O domínio não muda.
- Testes de arquitetura com ArchUnit são desejáveis para proteger essas fronteiras, mas são dependência nova. Ficam como sugestão pendente de autorização do Fabiano.

## Consequências

- As regras do design ficam testáveis com testes unitários puros, sem contexto Spring.
- Trocar o mock por banco de dados não altera o domínio nem os testes de domínio.
- Adicionar mobile é um novo adaptador de entrada sobre a mesma API e o mesmo domínio.

## Trade-offs

- Há mais mapeamentos entre DTOs e domínio do que em um CRUD simples. Cada endpoint precisa de tradução explícita.
- Para uma API com uma entidade e poucas regras, essa estrutura seria excesso. Aqui se justifica pelas regras de corte e de status.
