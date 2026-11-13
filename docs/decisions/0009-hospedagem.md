# 0009: Hospedagem da API no Render

Status: aceita

Data: 2026-11-12

## Contexto

A API precisa estar no ar para o front (ADR 0004) e, depois, o mobile consumirem. O projeto é de aprendizado, sem usuários reais nesta fase, e o autor não quer informar cartão de crédito. Os planos das plataformas foram conferidos em 12/11/2026 e **serão reconferidos no momento do deploy**, porque faixas grátis mudam com frequência.

## Decisão

- Hospedar a API no Render, no plano grátis, como serviço web com runtime Docker.
- Deploy automático a cada merge na `main`.

## Motivos

- Não exige cartão de crédito.
- Setup mais simples que o do Cloud Run.
- Suficiente para a fase atual, em que o mês é mockado em memória (ADR 0005).

## Trade-offs aceitos

- O serviço dorme após 15 minutos sem tráfego e leva cerca de 1 minuto para acordar. A primeira requisição depois da pausa é lenta.
- 512 MB de RAM e 0,1 CPU. Limita o que a aplicação pode fazer.
- O rollback pelo painel só alcança os 2 deploys anteriores.

## Alternativas consideradas

- Cloud Run: escala a zero e é bom tecnicamente, mas exige mais configuração e conta de faturamento.
- Railway: oferece apenas um crédito de teste de US$ 5, sem faixa grátis contínua.
- Fly.io: sem faixa grátis para contas novas.

## Consequências

- O empacotamento é um `Dockerfile` na raiz, o mesmo que serve para rodar a imagem localmente.
- A aplicação lê a porta da variável `PORT`, com 8080 como padrão local.
- O rollback será detalhado na PR do workflow, depois de conferir a documentação do Render. Esta decisão não define o procedimento.
- Migrar para outra plataforma exige só trocar o destino do deploy, porque a imagem Docker é padrão.
