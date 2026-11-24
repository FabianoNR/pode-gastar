# Pode Gastar

API HTTP que diz se uma compra cabe no orçamento do mês.

## Três camadas

- Card (efêmero): o que fazer agora e por quê.
- Spec (viva): o que o sistema faz hoje, em `docs/spec.md` e `docs/operacao.md`.
- `CLAUDE.md` (estável): como o agente trabalha.

O fluxo completo está na ADR 0015 (`docs/decisions/0015-fluxo-com-agentes-a-partir-do-card.md`).

## Mapa

- `docs/spec.md`: o comportamento visto de fora (contrato da API, regras, mensagens e critérios de aceite).
- `docs/operacao.md`: deploy, rollback, workflows e scripts.
- `docs/decisions/`: ADRs. Cada decisão nova vira um arquivo numerado.
- GitHub Project "Pode Gastar": os cards, com os campos Status e Situação.
- Card: uma issue do Project, com os critérios de aceite e a lista das seções da spec que podem mudar.
- `docs/design/`: congelado, só como histórico.
- `.claude/skills/`: skills das etapas antes do código (hoje, `entendimento`, `concepcao` e `refinamento`).
- Artefatos da issue: o Entendimento, a Concepção e o Refinamento ficam cada um num artefato da issue; o corpo do card guarda só o resumo e, do Refinamento, os blocos "Pronto para desenvolver" e "Roteiro de desenvolvimento".
- `.github/ISSUE_TEMPLATE/`: templates de issue (Story, Bug e Dívida).

## Trabalho a partir do card

1. Trabalhe a partir do card indicado. O trabalho de código só começa com Status Desenvolvimento ou adiante e Situação Normal (quem leva o card até lá é a skill de Refinamento, com a Definition of Ready cumprida), critérios de aceite e a lista das seções da spec. Se faltar algo, pare e avise. As skills trabalham antes disso: a de Entendimento em Backlog (inclusive em cards em Hold), a de Concepção em Pronto p/ Concepção e em Concepção (inclusive com Situação Blocked) e a de Refinamento em Pronto p/ Refinamento e em Refinamento (inclusive com Situação Blocked).
2. Os Status Refinamento e Desenvolvimento são movidos pela skill de Refinamento: para Refinamento ao começar e para Desenvolvimento ao concluir, com a confirmação do Fabiano. O agente de código não move o card para Desenvolvimento. Nunca mova um card para trás. Exceções, só por decisão do Fabiano: (a) a skill de Entendimento devolve o Status para Backlog no Hold; (b) a skill de Refinamento move para Pronto p/ Concepção quando o card volta à Concepção.
3. Crie a branch `<tipo>/<N>-slug` a partir da `main`, com o número do card (`feat`, `fix`, `docs` ou `chore`). Um card por PR.
4. Leia a spec antes de escrever código, e o `docs/operacao.md` quando o card tocar workflows ou scripts. Escreva os testes dos critérios do card antes da implementação. Rode e confirme que falham.
5. Atualize, na mesma PR do código, só as seções de `docs/spec.md` e `docs/operacao.md` que o card lista.
6. Se o card não cobre o caso, pare e devolva ao Fabiano: comente no card o que parou e espere. O Status não muda. Se a dúvida muda o comportamento visível do sistema, o Fabiano decide se ela volta à Concepção como card novo.
7. Faça commits pequenos com Conventional Commits. Nunca faça push na `main`. Abra a PR com "Closes #N".
8. Depois de abrir a PR, chame o subagent revisor com o diff, o texto do card e a correspondência entre o número provisório do card e o número definitivo dos critérios (ele não tem acesso ao GitHub: cole o texto no prompt). Poste o relatório dele como comentário na PR, com `gh pr comment`. O relatório separa:
   - Pendência: contradição com `docs/spec.md`, com `docs/operacao.md`, com o card ou com uma ADR (inclui o que o CLAUDE.md proíbe e as regras de escopo da spec do `.claude/agents/revisor.md`); violação de qualidade: item do checklist ou letra de SOLID do `.claude/agents/revisor.md` (ADR 0008).
   - Sugestão: todo o resto.

   Só pendência bloqueia a PR. Sugestão nunca vira card sozinha: liste as sugestões e o Fabiano decide. O padrão é descartar.
9. Mova o card para Aguardando revisão só quando a PR tiver o CI verde e o relatório do revisor não tiver pendência.
10. Ao terminar, informe o que mudou, o que foi testado e o que não foi.

## Pronto quando

Vale para todo card:

- Os testes dos critérios de aceite foram escritos antes da implementação e passam.
- O revisor de IA não tem pendência, e o relatório dele está na PR.
- `docs/spec.md` e `docs/operacao.md` foram atualizados nas seções que o card lista.
- O Fabiano fez o QA local no Postman.
- A PR está ligada ao card ("Closes #N") e tem o CI verde.

## Decisões durante o desenvolvimento

Cada decisão tomada durante o desenvolvimento vira um comentário no card, um por decisão, com: data, decisão, chapéu e motivo.

## Limites do agente

- Nunca altere a Situação do card. Nunca mova o card para Em revisão, Aguardando QA, Teste QA ou Done.
- As skills de Entendimento, Concepção e Refinamento movem o Status da própria etapa. Nenhuma skill altera a Situação nem fecha issue.
- O merge é do Fabiano, por squash, um por vez. O card vai para Done no merge.
- Branch com PR aberta recebe a `main` por merge, não por rebase. Sem force push.

## Stack

- Java 21 (LTS) com Maven.
- Spring Boot para a API HTTP.
- JUnit 5 para testes.
- Spotless para formatação, JaCoCo para cobertura.
- Docker para empacotar a aplicação.

## Comandos

- `./mvnw test`: roda os testes.
- `./mvnw verify`: testes, verificação de formatação e cobertura. É o comando que o CI usa.
- `./mvnw spotless:apply`: formata o código.

## Convenções

- Valores monetários usam `BigDecimal`, nunca `double` ou `float` (ver `docs/decisions/0002-dinheiro-com-bigdecimal.md`).
- Não adicione dependência nova sem perguntar.
- `docs/spec.md` e `docs/operacao.md` só mudam na PR do código, nas seções que o card lista. Diga na descrição da PR quais seções mudaram.
- Os critérios de aceite no card e nos artefatos têm numeração provisória. O número definitivo é dado na PR do código, ao escrevê-los na spec: o próximo livre da mesma série (CA ou CB), conferindo `docs/spec.md` e `docs/operacao.md`. A descrição da PR traz a correspondência entre o número do card e o número final (o revisor precisa dela).

## Qualidade de código

Clean Code e SOLID são critério obrigatório para todo código de produção e de teste. A decisão está em `docs/decisions/0008-qualidade-de-codigo.md`.

- Use nomes descritivos para classes, métodos, variáveis e testes. Variável de uma letra só em laço curto de índice.
- Identificadores de regra de negócio (CA1, CB2 etc.) não aparecem em nome de classe, método ou variável. Eles ficam no `@DisplayName` do teste, entre parênteses, no final do texto. Exemplo: `@DisplayName("Decisão é negada quando o saldo não cobre a compra (CA8)")`.
- O domínio não usa Spring, JPA nem adaptadores (ADR 0003). O `ArquiteturaHexagonalTest` impõe a regra.

## Proibido

- Ler ou escrever `.env`, credenciais e chaves.
- Implementar regra que não esteja em `docs/spec.md`, em `docs/operacao.md` ou no card.
- Mesclar PR com `./mvnw verify` falhando.
