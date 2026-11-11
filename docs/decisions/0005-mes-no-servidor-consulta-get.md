# 0005: Mês guardado no servidor, consulta por GET

Status: aceita

Data: 2026-11-11

## Contexto

O design aprovado diz que a pergunta "essa compra cabe?" é feita sobre o estado atual do mês, que o Fabiano registra ao longo do tempo. Essa pergunta não altera nada: não salva compra, não cria item e não muda status. A consulta precisa do mês como entrada, e o mês precisa existir no servidor para isso.

## Decisão

- O mês fica guardado no servidor, identificado por `mesId`.
- A consulta é `GET /meses/{mesId}/avaliacao?valor=&categoria=`.
- O recurso se chama `avaliacao`, e não `decisoes`, porque nada é salvo. A resposta é o resultado da avaliação de uma compra hipotética.
- O nome da compra não entra na consulta. Só valor e categoria afetam o resultado.

## Consequências

- A consulta é idempotente e segura para cache e repetição.
- Quem chama não precisa enviar o estado do mês a cada pergunta.

## Trade-offs

- O `mesId` depende da definição de grupo, que está fora de escopo nesta fase. A forma de identificar o mês fica para depois, quando grupo e permissões forem definidos.
- Enquanto isso, o mês é mockado em memória atrás da porta de entrada (ver ADR 0003).
