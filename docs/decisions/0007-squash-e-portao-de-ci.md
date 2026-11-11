# 0007: Merge por squash e portão de CI

Status: aceita

Data: 2026-11-11

## Contexto

A `main` precisa ter um histórico legível, com uma entrada por mudança, e nenhuma mudança pode entrar sem validação automática.

## Decisão

- A `main` só recebe mudança por PR.
- O merge é por squash. O título da PR vira a mensagem do commit na `main`, por isso o título segue Conventional Commits.
- O merge só acontece com CI verde. O GitHub não permite aprovar a própria PR, então a revisão é feita pelo autor no diff antes do merge, e a proteção da main não exige aprovação.
- Exceção registrada: a PR #3 (harness, Etapa 2) foi mesclada sem CI, porque tem só configuração e nenhum código Java, então `./mvnw verify` não tem como quebrar. O portão de CI vale a partir da Etapa 3.

## Consequências

- O squash remove os commits intermediários da PR da `main`. O detalhe fica na PR.
- A Etapa 3 deve ativar a proteção de branch, para que o portão deixe de depender de disciplina.
