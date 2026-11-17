# 0012: Limite de 20 itens nos grupos combinados

Status: aceita, pendente de confirmação na spec

Data: 2026-11-17

## Contexto

A spec v8 prevê o erro 422 para "grupo com mais de 20 itens ao calcular as sugestões" (`docs/spec.md`, contrato da API) e diz que o limite "continua valendo por grupo". Duas coisas a spec não define:

- quando a checagem acontece;
- se, na opção 2 da R7, o limite vale para `QUERO_FAZER` e `DEVO_FAZER` juntos (pendência registrada em "Abertos").

A busca de combinações testa todas as combinações do grupo (2^n), e o tempo dobra a cada item a mais. Na PR #20, o limite chegou a ser retirado dos itens em discussão e do `DEVO_FAZER` da compra `QUERO_FAZER`. Com isso, o custo ficou assim:

| Itens no grupo | Tempo da busca | Como foi obtido |
|---|---|---|
| 21 | cerca de 1,5 s | medido no teste |
| 25 | cerca de 25 s | estimado (dobra por item) |
| 30 | mais de 10 min | estimado (dobra por item) |
| 64 ou mais | erro 500 | a máscara de bits estoura, nenhuma combinação é testada e a busca não acha resultado |

O Fabiano decidiu, em 17/11/2026, voltar o limite para esses grupos e garantir que nenhum grupo acima do limite chegue à busca.

## Decisão

- Limite de 20 itens em todo grupo que passa pela busca de combinações.
- Quando a checagem acontece:
  - **Itens em discussão (grupo 0), em qualquer compra, e `DEVO_FAZER` (grupo 3) da compra `QUERO_FAZER`:** antes de qualquer combinação, mesmo que o grupo não seja usado. Isso inclui o caso em que a compra vai direto para a lista completa.
  - **`SERIA_LEGAL` e `QUERO_FAZER` na regra de corte:** quando o grupo precisa ser combinado. Um grupo que não é necessário para cobrir a falta não gera erro. É o comportamento que já existia.
  - **Opção 2 da R7:** `QUERO_FAZER` e `DEVO_FAZER` contados juntos, quando a opção é calculada. É o comportamento que já existia.
- A própria busca de combinações recusa um grupo acima do limite antes de começar. Assim, nenhum grupo com mais de 20 itens chega a ela, e o caso de 64 itens responde 422, nunca 500.
- A checagem acontece sempre que a compra não cabe no livre, mas cabe no total restante, ou seja, quando entra no cálculo de sugestões de corte (R3). Isso inclui a compra `SERIA_LEGAL` que terminaria em `NAO_CABE` por falta de itens seria legal (R6 e R11): com mais de 20 itens em discussão, ela responde 422. Compra `APROVADO` (R2) ou `NAO_CABE` pela R4 não passa pela checagem.

## Consequências

- Um mês com mais de 20 itens em discussão recebe 422 em toda compra que entra no cálculo de sugestões (R3), mesmo quando os itens em discussão não seriam usados. Isso inclui a compra `SERIA_LEGAL` que, sem o limite, terminaria em `NAO_CABE` pela R6.
- A checagem antecipada vale para dois grupos, e a checagem no momento da combinação vale para os outros. A diferença fica registrada aqui até a spec decidir.
- Os testes ficam em `LimiteDeItensPorGrupoTest` (domínio) e `LimiteDeItensHttpTest` (422 na API).

## Trade-offs

- O limite troca uma resposta lenta, ou um erro 500, por um 422 claro. O custo é recusar meses grandes que, em alguns casos, nem precisariam combinar o grupo.
- Uma busca sem custo exponencial dispensaria o limite, mas mudaria o algoritmo. Fica fora desta PR.
