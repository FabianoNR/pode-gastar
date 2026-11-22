# 0011: Ordem numérica natural no desempate por id entre combinações

Status: aceita, pendente de confirmação na spec

Data: 2026-11-17

## Contexto

A spec v8 desempata combinações do mesmo grupo pelo "menor id, na ordem alfabética dos ids" (`docs/spec.md`, R6, regra de corte, passo 3). Ela não define:

- como comparar duas combinações que têm vários ids cada uma;
- se "item-10" vem antes ou depois de "item-2". Na ordem alfabética pura, "item-10" vem antes.

O Fabiano decidiu, em 17/11/2026, na PR #20, usar a ordem numérica natural.

## Decisão

- Para comparar duas combinações, os ids de cada uma são ordenados e comparados um a um, até o primeiro diferente. Se todos os ids comuns forem iguais, a combinação com menos itens vem antes.
- A comparação de dois ids usa a ordem numérica natural: o id é dividido em trechos de dígitos e de texto. Trechos de dígitos comparam pelo valor, e trechos de texto comparam pelo texto. Assim, "item-2" vem antes de "item-10".
- Se dois ids só diferem por zeros à esquerda ("item-02" e "item-2"), o texto decide, para que dois ids diferentes nunca empatem.
- A regra fica na classe `OrdemNaturalDeIds`, no domínio.

## Consequências

- Esta interpretação vale só para o desempate entre combinações (R6, passo 3), que inclui a busca da opção 2 da R7.
- Os outros desempates por id continuam em ordem alfabética: a ordem dos itens dentro da sugestão (R6), as opções 1 e 3 (R7) e a lista completa (R7). Se a spec confirmar a ordem natural, esses pontos devem mudar juntos, em PR própria. A regra da PR própria para a spec foi substituída pela ADR 0015: a spec muda na PR do código, nas seções que o card lista.
- A spec descreve esta regra na [R6](../spec.md). A confirmação ou correção vem em PR de spec, separado do código. A regra da PR própria para a spec foi substituída pela ADR 0015: a spec muda na PR do código, nas seções que o card lista.

## Trade-offs

- A ordem natural combina com ids como "item-1", "item-2"… "item-10", que é o formato usado hoje. O custo é uma regra a mais no domínio e uma diferença, por enquanto, entre o desempate de combinações e os demais desempates por id.
