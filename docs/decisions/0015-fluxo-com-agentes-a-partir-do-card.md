# 0015: Fluxo de desenvolvimento com agentes a partir do card

Status: aceita. Substitui em parte a ADR 0006, no que é fluxo de trabalho e papel do revisor; o harness técnico da 0006 continua, com as permissões ampliadas por esta ADR.

Data: 2026-11-21

## Contexto

Até aqui, o `CLAUDE.md` exigia um design aprovado em `docs/design/` antes de mudar a spec, e PRs separadas para spec e código. A retrospectiva de 19/11/2026 apontou atrito e loops de conformidade entre spec, design e código. O entendimento já acontece nos cards do GitHub Project "Pode Gastar". Os documentos de design foram congelados na PR #30.

## Decisão

### Três camadas

- **Card (efêmero):** o que fazer agora e por quê. Uma issue no GitHub Project, com Issue type nativo (Story, Bug, Dívida), critérios de aceite e a lista das seções da spec que podem mudar.
- **Spec (viva):** o que o sistema faz hoje. `docs/spec.md` para o produto e `docs/operacao.md` para deploy e rollback.
- **`CLAUDE.md` (estável):** como o agente trabalha.

### Upstream e downstream

- **Upstream** (Backlog, Entendimento, Pronto p/ Concepção, Concepção, Pronto p/ Refinamento): conversa com o Fabiano. Termina num card pronto para refinar. As skills de Entendimento e Concepção ficam para a PR seguinte.
- **Downstream** (Refinamento em diante): o agente trabalha a partir do card indicado. Só começa por card com Situação Normal, Status Pronto p/ Refinamento ou adiante, critérios de aceite e lista das seções da spec; senão, para e avisa.
- O agente escreve primeiro os testes dos critérios e confirma que falham. Muda o código e, na mesma PR, só as seções da spec que o card lista. A lista do card é a autorização para mudar a spec. Se o card não cobre o caso, o agente comenta no card e devolve ao Fabiano.

### Estados

- O campo Status segue o fluxo. O agente move o card para Refinamento ao começar o plano, para Desenvolvimento ao começar a codar e para Aguardando revisão quando a PR tem CI verde e o relatório do revisor não tem pendência.
- O agente nunca move para Em revisão, Aguardando QA, Teste QA nem Done.
- O campo Situação (Normal, Hold, Blocked) é separado do Status e é de responsabilidade do Fabiano. O agente não o altera.
- Campos do Project: Prioridade, Porte, Origem, Versão e Motivo. O tipo do card é o Issue type nativo da issue, não um campo do Project.

### Cadência, revisão e merge

- Cadência card a card: um card por PR, em branch `<tipo>/<N>-slug` com o número do card (`feat`, `fix`, `docs`, `chore`).
- A cadência fica preparada para versão: todo card tem ID, o gatilho do deploy fica isolado no workflow, o campo Versão já existe e o estado "Pronto para Produção" fica reservado, sem uso por enquanto.
- O subagent revisor, somente leitura, compara o diff com a spec, com o `docs/operacao.md` e com o card. Ele roda depois de abrir a PR e antes de mover o card para Aguardando revisão. A sessão principal posta o relatório como comentário na PR com `gh pr comment`.
- QA local antes do merge.
- Merge por squash, feito pelo Fabiano (ADR 0007), um por vez. A PR traz "Closes #N", e o card vai para Done no merge.
- Branch com PR aberta recebe a `main` por merge, não por rebase. Sem force push.

### Permissões

O `.claude/settings.json` passa a liberar `gh issue create`, `edit` e `comment`, `gh pr comment`, leitura e escrita de itens no Project e `gh api -X PATCH` em issues do repositório. O PATCH existe para o Issue type, porque o `gh` 2.45 não tem a opção `--type`, e como alternativa se `gh issue edit` falhar. Ficam negados `gh api graphql`, `gh api` com merge ou DELETE, e a exclusão de Project, campo, item e issue.

## Consequências

- Uma PR por card, com spec e código juntos. O card é a autorização para mudar a spec.
- O revisor depende de receber o texto do card no prompt. Sem o card, ele pede o card antes de continuar.
- `docs/design/` fica como histórico.
- Riscos conhecidos:
  - O `gh project item-edit` também permite alterar a Situação do card. Hoje só a instrução do `CLAUDE.md` impede o agente de fazer isso.
  - O bloqueio das permissões é por texto de comando e pode ser contornado. O bloqueio de `gh pr merge` continua valendo, mas liberar `gh api`, mesmo restrito, amplia o que dá para fazer. A proteção real continua sendo a regra de branch no GitHub (ADR 0006).

## Trade-offs

- Sem o portão de design, a qualidade do card passa a ser o ponto de controle. Card sem critérios ou sem seções da spec trava o downstream.
- Spec e código na mesma PR deixam a revisão maior, mas evitam a spec e o código fora de sincronia entre duas PRs.
