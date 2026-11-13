# pode-gastar

API HTTP que diz se uma compra cabe no orçamento do mês.

## Documentação

- Spec: [docs/spec.md](docs/spec.md)
- Design aprovado: [docs/design/](docs/design/)
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

Todo merge na `main` passa pelo pipeline do GitHub Actions (`.github/workflows/ci.yml`), nesta ordem:

1. **verify**: roda `./mvnw verify` (testes, formatação e cobertura).
2. **publish**: se o `verify` passou, gera a imagem Docker e publica no GHCR (`ghcr.io/fabianonr/pode-gastar`) com duas tags: o SHA do commit e `main`.
3. **deploy**: chama o deploy hook do Render apontando para a imagem do SHA. O Render baixa a imagem e troca a versão no ar.
4. **smoke-test**: consulta `https://pode-gastar-api.onrender.com/health` até 20 vezes, a cada 30 segundos, esperando `200` com `"UP"`.

O deploy é automático. Ele exige o segredo `RENDER_DEPLOY_HOOK_URL` no GitHub (Settings > Secrets and variables > Actions). Sem o segredo, a imagem é publicada, mas o job `deploy` pula com um aviso.

Só existem imagens para commits publicados depois da PR #15 (commit `06943bd`). Commits anteriores não têm imagem no GHCR, então não servem para rollback.

## Rollback

Use quando a versão nova no ar estiver quebrada e precisar voltar para a anterior.

1. Abra a aba **Actions** do repositório e escolha o workflow **Rollback**.
2. Clique em **Run workflow**.
3. No campo `sha`, cole o SHA completo, com 40 caracteres, do último commit que funcionava.
4. Clique em **Run workflow** de novo e espere o job ficar verde.

Para pegar o SHA, abra a página do commit no GitHub. O hash completo aparece na barra de endereço, depois de `/commit/`. Use o hash completo, não o curto de 7 caracteres.

O workflow faz, nesta ordem:

- confere que a imagem `ghcr.io/fabianonr/pode-gastar:<sha>` existe. Se não existir, para com erro e não altera nada;
- move a tag `main` para essa imagem. Isso é necessário porque uma reinicialização no Render usa a imagem configurada no serviço, que é `main`;
- dispara o deploy no Render com essa imagem;
- consulta `/health` com a mesma regra do smoke test.

O smoke test só confirma que `/health` responde. Ele não diz qual versão está no ar. Para confirmar, abra o serviço no Render, na aba **Events**, e veja se o último deploy usou a imagem com o SHA escolhido.

Alternativa sem o workflow: crie um commit que desfaz a mudança problemática (`git revert <sha-ruim>`), abra uma PR e faça o merge. O pipeline completo roda de novo. É mais lento, mas o histórico fica claro.

Não é possível voltar para um commit anterior à PR #15, porque ele não tem imagem publicada.

**Estado do teste:** o workflow foi executado com o SHA `06943bd`, que já é a `main` atual. Isso testou a verificação da imagem, a troca da tag, o deploy e o smoke test, mas não testou a volta para uma versão anterior. Um SHA sem imagem publicada parou no primeiro passo, como esperado. Voltar para um commit anterior ainda não foi testado.

## Commits

Os commits seguem [Conventional Commits](https://www.conventionalcommits.org/pt-br/). O tipo vem antes dos dois-pontos:

- `feat: adiciona cálculo do livre no mês`: funcionalidade nova.
- `fix: corrige arredondamento do saldo restante`: correção de bug.
- `docs: atualiza README`: só documentação.
- `chore: gera scaffold Spring Boot com Maven wrapper`: tarefa de manutenção sem efeito no comportamento.
- `ci: adiciona workflow verify`: mudança no pipeline de CI.

O título da PR vira a mensagem do commit na `main`, porque o merge é por squash (ver [ADR 0007](docs/decisions/0007-squash-e-portao-de-ci.md)).
