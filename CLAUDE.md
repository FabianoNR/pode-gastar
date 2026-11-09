# Pode Gastar

API HTTP que diz se uma compra cabe no orçamento do mês.

## Fonte da verdade

- Spec: `docs/spec.md`. Regra de negócio que não estiver na spec não deve ser implementada.
- Decisões de arquitetura: `docs/decisions/`. Cada decisão nova vira um arquivo numerado.

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

Esses comandos passam a existir depois do scaffold do projeto, que é o primeiro passo da implementação.

## Fluxo de trabalho obrigatório

1. Leia `docs/spec.md` antes de escrever qualquer código. Se a spec não cobre o que foi pedido, pare e pergunte.
2. Escreva os testes a partir dos critérios de aceite antes da implementação. Rode e confirme que falham.
3. Trabalhe em uma fatia por vez, em branch própria (`feat/...`, `fix/...`, `docs/...`).
4. Faça commits pequenos com Conventional Commits.
5. Nunca faça push direto na `main`. Abra PR.
6. Ao terminar uma fatia, pare e informe: o que mudou, o que foi testado e o que não foi testado.

## Convenções

- Valores monetários usam `BigDecimal`, nunca `double` ou `float` (ver `docs/decisions/0002-dinheiro-com-bigdecimal.md`).
- Não adicione dependência nova sem perguntar.
- Não altere a spec sem avisar. Mudança de spec vai em PR próprio, separado da implementação.

## Proibido

- Ler ou escrever `.env`, credenciais e chaves.
- Implementar regra de negócio que não esteja em `docs/spec.md`.
- Mesclar PR com `./mvnw verify` falhando.
