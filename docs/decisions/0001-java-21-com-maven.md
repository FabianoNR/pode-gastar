# 0001: Java 21 (LTS) com Maven

Status: aceita

Data: 2026-11-09

## Contexto

O projeto é o primeiro com Claude Code e o objetivo principal é praticar um ciclo maduro de desenvolvimento com IA: spec, testes, PR, CI e deploy. A linguagem não é o foco, então a escolha deve reduzir atrito. O autor já conhece Java e se sente mais seguro com ela. Java e Spring aparecem com frequência em vagas de backend no mercado brasileiro.

## Decisão

- Linguagem: Java 21, versão LTS.
- Build e dependências: Maven, com o wrapper `mvnw` versionado no repositório.
- API: Spring Boot.
- Testes: JUnit 5.
- Formatação: Spotless. Cobertura: JaCoCo.

## Consequências

- A spec não depende da linguagem, então as regras de negócio continuam válidas se a stack mudar.
- Java exige mais cerimônia que Python. Isso aumenta o peso das convenções e do `CLAUDE.md`, o que serve bem ao objetivo do projeto.
- Uma atualização para uma LTS mais nova fica como decisão separada, em um novo ADR.
