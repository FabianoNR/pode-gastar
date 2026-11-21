# pode-gastar

API HTTP que diz se uma compra cabe no orçamento do mês.

## Documentação

- Spec: [docs/spec.md](docs/spec.md)
- Deploy e rollback: [docs/operacao.md](docs/operacao.md)
- Design congelado, histórico: [docs/design/](docs/design/)
- Decisões de arquitetura: [docs/decisions/](docs/decisions/)

## Como rodar

Pré-requisito: Java 21. O Maven não precisa estar instalado, porque o projeto usa o Maven Wrapper.

```bash
./mvnw verify
```

Roda compilação, testes, verificação de formatação (Spotless) e relatório de cobertura (JaCoCo em `target/site/jacoco/`). É o mesmo comando que o CI usa.

```bash
./mvnw spotless:apply
```

Formata o código com google-java-format.

## Deploy

A API roda no Render, num serviço construído a partir deste repositório: runtime Docker com o `Dockerfile` da raiz, branch `main` e Auto-Deploy desligado. O build acontece no próprio Render, a cada deploy. Ver [ADR 0014](docs/decisions/0014-render-construido-do-repositorio.md).

Quem dispara o deploy é o GitHub Actions (`.github/workflows/ci.yml`):

1. **verify**: roda `./mvnw verify` (testes, formatação e cobertura). Roda em toda PR e em todo push na `main`.
2. **deploy**: só em push na `main`, depois do `verify`. Chama o deploy hook do Render e consulta o `/health` até ele mostrar o commit que acabou de ser mesclado, com prazo de 10 minutos. Se o prazo acabar, o job falha.

As regras do deploy, da espera pelo `/health` e dos tempos limite estão em [docs/operacao.md](docs/operacao.md).

### Segredos

Ficam em Settings > Secrets and variables > Actions > Secrets, no repositório:

| Segredo | Usado por |
| --- | --- |
| `RENDER_DEPLOY_HOOK_URL` | deploy |
| `SERVICE_URL` | deploy e rollback (URL base do serviço no Render) |
| `RENDER_API_KEY` | rollback |
| `RENDER_SERVICE_ID` | rollback |

Segredo ausente ou vazio faz o job falhar no início, com uma mensagem que diz qual segredo falta, antes de chamar o Render ou o `/health`.

## Rollback

Use quando a versão nova no ar estiver quebrada e precisar voltar para uma anterior.

1. Abra a aba **Actions** do repositório e escolha o workflow **Rollback**.
2. Clique em **Run workflow**, com a branch `main` selecionada. Em outra branch, o job aparece como pulado.
3. No campo `sha`, cole o SHA completo, com 40 caracteres, do commit que deve voltar ao ar.
4. Clique em **Run workflow** de novo e espere o job ficar verde.

Para pegar o SHA, abra a página do commit no GitHub. O hash completo aparece na barra de endereço, depois de `/commit/`. Use o hash completo, não o curto de 7 caracteres.

O workflow pede ao Render, pela API, o deploy desse commit e consulta o `/health` até ele mostrar o SHA pedido, com prazo de 10 minutos. Job verde significa que o commit pedido está no ar. Para conferir à mão, abra `<SERVICE_URL>/health` e veja o campo `commit`:

```json
{"status": "UP", "commit": "<sha pedido>"}
```

O rollback só funciona para commits a partir da PR #25 (`a28a8af`). Commits anteriores não expõem o commit no `/health`, e o job falha por prazo mesmo com o deploy feito.

As regras do rollback estão em [docs/operacao.md](docs/operacao.md) e na [ADR 0013](docs/decisions/0013-rollback-por-api-render.md).

Alternativa sem o workflow: crie um commit que desfaz a mudança problemática (`git revert <sha-ruim>`), abra uma PR e faça o merge. O pipeline completo roda de novo. É mais lento, mas o histórico fica claro.

**Estado do teste:** em 18/11/2026, o rollback foi executado em produção nas duas direções, de `ff55e0c` para `a28a8af` e de volta para `ff55e0c`, cerca de 1 minuto cada, com o commit conferido no `/health`.

## Commits

Os commits seguem [Conventional Commits](https://www.conventionalcommits.org/pt-br/). O tipo vem antes dos dois-pontos:

- `feat: adiciona cálculo do livre no mês`: funcionalidade nova.
- `fix: corrige arredondamento do saldo restante`: correção de bug.
- `docs: atualiza README`: só documentação.
- `chore: gera scaffold Spring Boot com Maven wrapper`: tarefa de manutenção sem efeito no comportamento.
- `ci: adiciona workflow verify`: mudança no pipeline de CI.

O título da PR vira a mensagem do commit na `main`, porque o merge é por squash (ver [ADR 0007](docs/decisions/0007-squash-e-portao-de-ci.md)).
