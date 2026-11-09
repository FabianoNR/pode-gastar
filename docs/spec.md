# Spec: Pode Gastar (v0)

Status: rascunho para revisão. As regras de negócio são provisórias e serão revisadas depois da v0.

## Problema

Antes de fazer uma compra, quero saber rapidamente se ela cabe no orçamento do mês. A resposta deve ser "aprovado", "atenção" ou "recusado", com o motivo e o saldo que sobra.

## Escopo da v0

- Entrada: renda mensal, gastos fixos do mês, reserva mínima e valor da compra. Valores em reais, com duas casas decimais.
- Saída: decisão, motivo e saldo restante após a compra.
- Interface: API HTTP com um endpoint de decisão e um endpoint de saúde.

## Regras de negócio (provisórias)

1. `saldo_livre = renda - gastos_fixos - reserva_minima`
2. Se `saldo_livre <= 0`, a decisão é `RECUSADO`, com o motivo "sem saldo livre no mês".
3. Se `valor <= 50%` do `saldo_livre`, a decisão é `APROVADO`.
4. Se `50% < valor <= 100%` do `saldo_livre`, a decisão é `ATENCAO`.
5. Se `valor > saldo_livre`, a decisão é `RECUSADO`, com o motivo "compra excede o saldo livre".
6. `saldo_restante = saldo_livre - valor`, arredondado em duas casas.
7. Valor da compra igual ou menor que zero é entrada inválida.

## Contrato da API

`POST /decisoes`

Entrada:

```json
{
  "renda": "5000.00",
  "gastosFixos": "2500.00",
  "reservaMinima": "1000.00",
  "valorCompra": "500.00"
}
```

Saída (200):

```json
{
  "decisao": "APROVADO",
  "motivo": "compra dentro de 50% do saldo livre",
  "saldoRestante": "1000.00"
}
```

Entrada inválida: 400, com a mensagem do campo.

`GET /health`: 200 com `{"status": "UP"}`.

## Critérios de aceite

- **CA1.** Dado renda de 5000, gastos fixos de 2500, reserva de 1000 e compra de 500, quando eu pedir uma decisão, então a decisão é `APROVADO` e o saldo restante é 1000.00.
- **CA2.** Dado o mesmo cenário e compra de 1000, quando eu pedir uma decisão, então a decisão é `ATENCAO` e o saldo restante é 500.00.
- **CA3.** Dado o mesmo cenário e compra de 1600, quando eu pedir uma decisão, então a decisão é `RECUSADO` com o motivo "compra excede o saldo livre".
- **CA4.** Dado renda de 2000, gastos fixos de 2500 e reserva de 1000, quando eu pedir uma decisão, então a decisão é `RECUSADO` com o motivo "sem saldo livre no mês".
- **CA5.** Dado valor de compra zero ou negativo, quando eu pedir uma decisão, então a resposta é 400.
- **CA6.** Quando eu chamar `GET /health`, então a resposta é 200 com `{"status": "UP"}`.

## Fora de escopo da v0

- Compras parceladas e gastos com cartão de crédito.
- Planejamento de vários meses.
- Persistência de dados e histórico de decisões.
- Autenticação e múltiplos usuários.
- Interface gráfica e aplicativo mobile.

## Perguntas em aberto

- Os percentuais de 50% e 100% devem ser fixos ou configuráveis?
- Gastos variáveis já realizados no mês devem entrar no cálculo?
- Parcelas de compras anteriores devem ser descontadas do saldo livre?
