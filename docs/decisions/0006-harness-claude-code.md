# 0006: Harness mínimo do Claude Code

Status: aceita

Data: 2026-11-11

## Contexto

O agente trabalha no computador do Fabiano e pode executar comandos e editar arquivos. As regras de trabalho precisam estar versionadas no repositório, e os comandos perigosos precisam estar bloqueados por configuração, não apenas por instrução.

## Decisão

- `CLAUDE.md` na raiz define a fonte da verdade, o fluxo obrigatório e as proibições.
- `.claude/settings.json` libera: `./mvnw`, `git` (status, diff, log, show, branch, checkout, switch, add, commit, fetch, pull, push, stash) e `gh` (pr create, view, list, status, checks, diff; issue view e list; repo view).
- `.claude/settings.json` nega: leitura e edição de `.env`, `*.pem`, `*.key` e `credentials*`; `git reset --hard`, `git clean`, `git checkout --` e `git checkout .`; `git branch -D`; `git stash drop` e `clear`; push forçado; push para `main`; `gh pr merge`; `gh repo delete`.
- Hook `PostToolUse` após Edit, Write e MultiEdit roda `./mvnw -q spotless:apply`, se `./mvnw` existir.
- Subagent revisor em `.claude/agents/`, com acesso somente de leitura, compara o diff com a spec.

## Consequências

- O agente não mescla PR. O merge é feito pelo Fabiano no GitHub.
- Negações por padrão de texto podem ser contornadas. A proteção real da `main` é a regra de branch no GitHub, prevista na Etapa 3.
- Até a Etapa 3, o hook não faz nada, porque ainda não existe `./mvnw`.
