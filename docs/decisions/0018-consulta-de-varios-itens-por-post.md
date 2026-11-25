# 0018: Consulta de vários itens por POST

Status: aceita. Substitui em parte as ADRs 0005 e 0017, só no método da consulta de vários itens.

Data: 2026-11-25

## Contexto

As ADRs 0005 e 0017 fixaram a consulta de uma compra em `GET /meses/{mesId}/avaliacao?valor=&categoria=&intensidade=`, com o argumento de que a consulta não altera nada. O card #67 pede a consulta de vários itens de uma vez: uma lista de 1 a 20 itens, cada um com nome, valor, categoria e intensidade opcional, avaliados sozinhos contra o mesmo mês.

Uma lista de objetos na query string é ilegível, sujeita a erro de codificação (nomes com espaço, acento e aspas) e a limite de tamanho na URL.

## Decisão

- A consulta de vários itens é `POST /meses/{mesId}/avaliacoes`, com o pedido no corpo, em JSON: `{"itens": [{"nome", "valor", "categoria", "intensidade"}]}`.
- O POST não cria nem altera nada. O recurso se chama `avaliacoes` porque devolve uma avaliação por item, como a `avaliacao` do GET.
- O corpo é lido e validado no adaptador web, sem dependência nova. `valor` vem como texto, como manda a ADR 0002, e é lido como `BigDecimal`, do mesmo jeito que o GET lê `?valor=`. `intensidade` vem como número JSON.
- A consulta de uma compra continua `GET /meses/{mesId}/avaliacao` (ADRs 0005 e 0017), sem mudança.
- Continua valendo da ADR 0005: o mês fica no servidor, identificado por `mesId`.

## Consequências

- A consulta de vários itens não é cacheável nem repetível por HTTP como o GET: proxies e navegadores tratam POST como operação que altera estado. Repetir o pedido é seguro, porque nada muda no servidor, mas quem chama precisa saber disso pelo contrato, não pelo método.
- O front (ADR 0004) precisa da rota nova para usar a consulta de vários itens. O GET segue igual, então nada quebra enquanto ele não muda.
- O contrato da API passa a ter dois métodos para a mesma pergunta, um por compra e um por lista. A spec descreve os dois.
