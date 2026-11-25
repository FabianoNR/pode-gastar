# 0017: Intensidade da compra na consulta

Status: aceita. Substitui em parte a ADR 0005, no que é a forma da consulta e as entradas que afetam o resultado. Substituída em parte pela ADR 0018: a consulta de vários itens usa POST.

Data: 2026-11-24

## Contexto

A ADR 0005 fixou a consulta em `GET /meses/{mesId}/avaliacao?valor=&categoria=` e disse que só valor e categoria afetam o resultado. O card #63 pede que a consulta também receba o quanto a compra importa (a intensidade), para que a resposta mostre quando os itens que seriam cortados são mais desejados que a própria compra.

## Decisão

- A consulta passa a ser `GET /meses/{mesId}/avaliacao?valor=&categoria=&intensidade=`.
- `intensidade` é opcional, um inteiro de 1 a 5. Sem ela, vale 1. Fora de 1 a 5 ou não inteira, a resposta é 400, apontando o campo `intensidade`.
- Valor, categoria e intensidade afetam o resultado. A intensidade não muda a decisão, as sugestões nem a conta dos cortes. Ela só muda, na decisão `CABE_COM_CORTES`, o campo `itensMaisDesejadosQueACompra` e a mensagem em `avisos` (spec, R12).
- O nome da compra continua fora da consulta.
- Continua valendo da ADR 0005: o mês fica no servidor, o método é GET e o recurso se chama `avaliacao`.

## Consequências

- A consulta continua idempotente e segura para cache e repetição. A chave de cache passa a incluir a intensidade.
- Consultas antigas, sem o parâmetro, continuam válidas. Elas recebem o campo novo e, quando ele não está vazio, o aviso de que a intensidade foi considerada 1.
- O front (ADR 0004) precisa conhecer o parâmetro e o campo novo para usá-los. Como os dois são aditivos, nada quebra enquanto ele não muda.
