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

- **Upstream** (Backlog, Entendimento, Pronto p/ Concepção, Concepção, Pronto p/ Refinamento): conversa com o Fabiano. Termina num card pronto para refinar. Duas skills conduzem essa conversa (ver "Skills upstream").
- **Downstream** (Refinamento em diante): o agente trabalha a partir do card indicado. Só começa por card com Situação Normal, Status Pronto p/ Refinamento ou adiante, critérios de aceite e lista das seções da spec; senão, para e avisa.
- O agente escreve primeiro os testes dos critérios e confirma que falham. Muda o código e, na mesma PR, só as seções da spec que o card lista. A lista do card é a autorização para mudar a spec. Se o card não cobre o caso, o agente comenta no card e devolve ao Fabiano.

### Skills upstream

As skills ficam em `.claude/skills/`. Cada uma move o Status só da própria etapa, nunca altera a Situação e nunca fecha issue.

- **Entendimento** (`entendimento`, chapéu PM): parte de um pedido solto ou de um card em Backlog. Move o Status de Backlog para Entendimento e termina com a decisão do Fabiano:
  - Seguir: grava a Prioridade e move para Pronto p/ Concepção.
  - Hold: comenta o Motivo no card e devolve o Status para Backlog. É a única exceção à regra "nunca mover para trás": Entendimento é estado ativo, e um card parado em Hold não pode contar como tempo ativo. O Fabiano muda a Situação para Hold.
  - Descartar: comenta o Motivo; o Fabiano fecha a issue como "not planned" e a automação leva o card para Done.
- **Concepção** (`concepcao`): parte de um card em Pronto p/ Concepção com o Entendimento decidido como Seguir. Move o Status para Concepção, define solução, critérios de aceite, porte, riscos e seções da spec autorizadas, grava o Porte e move para Pronto p/ Refinamento. Elefante não passa adiante: fica em Concepção até o fatiamento.
- **Retomada de card Blocked em Concepção:** quando uma dúvida crítica depende de pessoa, de teste real ou de outra área, a skill grava o que já foi decidido no artefato "Concepção", o Status continua em Concepção e o Fabiano muda a Situação para Blocked. Ao retomar, a skill lê o artefato "Concepção" existente, parte do que já foi decidido e pergunta só o que falta.
- **Fonte da Demanda:** o Entendimento propõe o valor entre as opções do campo do Project e grava depois da confirmação do Fabiano. Em card existente com o campo já preenchido, não mexe. Se o campo não existir, avisa.

### Artefatos da issue

O detalhe de cada etapa upstream fica num artefato da issue, em Markdown: "Entendimento" e "Concepção". O corpo do card guarda só o resumo que o downstream usa: o bloco "Resumo do Entendimento" e o bloco "Concepção" (solução, critérios de aceite, porte, riscos, seções da spec autorizadas). Cada skill só altera o próprio bloco e não toca o texto da criação. Para refazer, o artefato é editado, nunca apagado.

### Estados

- O campo Status segue o fluxo. O agente move o card para Refinamento ao começar o plano, para Desenvolvimento ao começar a codar e para Aguardando revisão quando a PR tem CI verde e o relatório do revisor não tem pendência.
- O agente nunca move para Em revisão, Aguardando QA, Teste QA nem Done.
- O campo Situação (Normal, Hold, Blocked) é separado do Status e é de responsabilidade do Fabiano. O agente não o altera.
- Campos do Project: Prioridade, Porte, Fonte da Demanda, Versão e Motivo. O tipo do card é o Issue type nativo da issue, não um campo do Project.

### Cadência, revisão e merge

- Cadência card a card: um card por PR, em branch `<tipo>/<N>-slug` com o número do card (`feat`, `fix`, `docs`, `chore`).
- A cadência fica preparada para versão: todo card tem ID, o gatilho do deploy fica isolado no workflow, o campo Versão já existe e o estado "Pronto para Produção" fica reservado, sem uso por enquanto.
- O subagent revisor, somente leitura, compara o diff com a spec, com o `docs/operacao.md` e com o card. Ele roda depois de abrir a PR e antes de mover o card para Aguardando revisão. A sessão principal posta o relatório como comentário na PR com `gh pr comment`.
- QA local antes do merge.
- Merge por squash, feito pelo Fabiano (ADR 0007), um por vez. A PR traz "Closes #N", e o card vai para Done no merge.
- Branch com PR aberta recebe a `main` por merge, não por rebase. Sem force push.

### Permissões

O `.claude/settings.json` passa a liberar `gh issue create`, `edit` e `comment`, `gh pr comment`, leitura e escrita de itens no Project, `gh issue artifact create`, `edit`, `view` e `list`, e `gh api -X PATCH` em issues do repositório. O `gh` instalado (2.105.0) tem `--type` em `gh issue create` e `gh issue edit`. O PATCH continua em uso na skill de Entendimento para aplicar o Issue type e fica como alternativa se `gh issue edit` falhar. Ficam negados `gh api graphql`, `gh api` com merge ou DELETE, `gh issue artifact delete`, e a exclusão de Project, campo, item e issue.

## Consequências

- Uma PR por card, com spec e código juntos. O card é a autorização para mudar a spec.
- O revisor depende de receber o texto do card no prompt. Sem o card, ele pede o card antes de continuar.
- `docs/design/` fica como histórico.
- Riscos conhecidos:
  - O `gh project item-edit` também permite alterar a Situação do card. Hoje só a instrução do `CLAUDE.md` e das skills impede o agente de fazer isso.
  - O bloqueio das permissões é por texto de comando e pode ser contornado. O deny de DELETE só pega o `-X` (ou `--method`) logo depois de `api`; com a opção em outra posição, ele não casa. E o `*` no allow do PATCH aceita opções extras no fim do comando. O bloqueio de `gh pr merge` continua valendo, mas a proteção real é a regra de branch no GitHub (ADR 0006).
  - O PATCH em issue pode fechar a issue se `state` for enviado, e a automação "Item closed" do Project manda o card para Done. As skills proíbem enviar `state`.
  - A edição do corpo do card grava o corpo inteiro, não só o bloco da skill. As skills comparam o corpo antigo com o novo antes de gravar; o backup é o histórico de edições da issue no GitHub.
  - Os artefatos da issue estão em public preview: o comando e o comportamento podem mudar. Eles ficam visíveis para quem vê a issue.

## Trade-offs

- Sem o portão de design, a qualidade do card passa a ser o ponto de controle. Card sem critérios ou sem seções da spec trava o downstream.
- Spec e código na mesma PR deixam a revisão maior, mas evitam a spec e o código fora de sincronia entre duas PRs.
