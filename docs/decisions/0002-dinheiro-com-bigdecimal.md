# 0002: Valores monetários com BigDecimal

Status: aceita

Data: 2026-11-09

## Contexto

`double` e `float` armazenam números em base binária. Valores como 0.10 não têm representação exata, e somas repetidas acumulam erro. Em um sistema que responde se o dinheiro cabe no orçamento, um centavo de diferença pode mudar a decisão.

## Decisão

- Todo valor monetário usa `java.math.BigDecimal`.
- Escala fixa de duas casas decimais.
- Arredondamento com `RoundingMode.HALF_EVEN` (arredondamento bancário), definido em um único lugar.
- Comparações usam `compareTo`, nunca `equals`, porque `equals` considera a escala.
- Entrada e saída da API trafegam como string, para não passar por ponto flutuante no JSON.

## Consequências

- Os testes de regra devem cobrir valores de centavos nas fronteiras de decisão definidas na spec.
- Qualquer `double` ou `float` em código de valor monetário é erro de revisão e deve ser recusado no PR.
