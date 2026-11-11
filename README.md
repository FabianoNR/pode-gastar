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

## Commits

Os commits seguem [Conventional Commits](https://www.conventionalcommits.org/pt-br/). O tipo vem antes dos dois-pontos:

- `feat: adiciona cálculo do livre no mês`: funcionalidade nova.
- `fix: corrige arredondamento do saldo restante`: correção de bug.
- `docs: atualiza README`: só documentação.
- `chore: gera scaffold Spring Boot com Maven wrapper`: tarefa de manutenção sem efeito no comportamento.
- `ci: adiciona workflow verify`: mudança no pipeline de CI.

O título da PR vira a mensagem do commit na `main`, porque o merge é por squash (ver [ADR 0007](docs/decisions/0007-squash-e-portao-de-ci.md)).
