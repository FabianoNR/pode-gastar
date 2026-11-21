# 0010: Itens com valor de corte zero (comportamento conhecido)

Status: aceita como comportamento conhecido, pendente de decisão na spec

Data: 2026-11-17

## Contexto

A spec v8 só exclui dos grupos de corte o item `PARCIAL` que já gastou além do previsto, porque cortá-lo não libera valor (`docs/spec.md`, R6, "Cortável de cada item"). Ela não diz nada sobre os itens cujo valor de corte é exatamente zero.

O código da Etapa C (PR #20) trata esses itens de forma diferente conforme o status:

- `PENDENTE` com valor estimado zero: fica de fora dos grupos de corte e da lista completa.
- `PARCIAL` com valor realizado igual ao planejado: fica de fora dos grupos de corte e da lista completa.
- `EM_DISCUSSAO` com valor estimado zero: entra no grupo 0 e aparece na lista completa.

O revisor apontou essa diferença na PR #20. O Fabiano decidiu, em 17/11/2026, não mudar o código agora.

## Decisão

- O comportamento acima é mantido sem mudança.
- A inconsistência fica registrada aqui como comportamento conhecido, até que a spec decida como tratar item de valor de corte zero.

## Consequências

- Um item em discussão de valor zero nunca é escolhido na sugestão, porque não ajuda a cobrir a falta e só aumentaria a soma das intensidades. Ele pode, porém, aparecer na lista completa.
- Quando a spec decidir, a mudança vem em PR própria, com teste para os três casos. A regra da PR própria para a spec foi substituída pela ADR 0015: a spec muda na PR do código, nas seções que o card lista.

## Trade-offs

- Manter o código evita implementar regra que a spec não tem. O custo é conviver com dois tratamentos diferentes para o mesmo caso de valor zero.
