# 0008: Qualidade de código com Clean Code e SOLID

Status: aceita

Data: 2026-11-12

## Contexto

O projeto é escrito em grande parte com apoio de IA, que produz código rápido e plausível, mas sem critério próprio de qualidade. Sem um padrão escrito, cada fatia pode usar nomes, tamanhos de método e responsabilidades diferentes. Os critérios de aceite (CA1, CB2 etc.) também tendem a vazar para nomes de classe, método e variável, o que prende o código a um documento que muda por PR próprio.

## Decisão

- Clean Code e SOLID são critério obrigatório para todo código de produção e de teste.
- Classes, métodos, variáveis e testes têm nomes descritivos. Variável de uma letra só aparece em laço curto de índice.
- Identificadores de regra de negócio (CA1, CB2 etc.) não aparecem em nome de classe, método ou variável. A rastreabilidade para a spec fica no `@DisplayName` do teste, entre parênteses, no final do texto. Exemplo: `@DisplayName("Decisão é negada quando o saldo não cobre a compra (CA8)")`.
- O domínio não usa Spring, JPA nem adaptadores, conforme a ADR 0003. Um teste de ArchUnit que impõe essa regra entra em fatia própria.
- O subagent revisor ganha um checklist com esses itens e com os critérios de SOLID aplicáveis a cada diff. Ele continua somente leitura.

## Consequências

- Achar o teste de um critério passa a ser uma busca pelo identificador entre parênteses no `@DisplayName`, por exemplo `(CA8)`. O nome do método descreve o comportamento e não muda se a numeração da spec mudar.
- O `@DisplayName` é texto livre, e o compilador não confere o identificador. A conferência fica com a revisão.
- Código e testes escritos antes desta decisão podem não seguir a convenção. Eles são ajustados em PR próprio, sem misturar com mudança de comportamento.
- O `CLAUDE.md` resume a regra, e este documento guarda o motivo.

## Trade-offs

- Nomes descritivos e responsabilidades pequenas geram mais classes e mais linhas do que o código mais curto possível.
- SOLID aplicado sem critério vira abstração desnecessária. Por isso o revisor só cobra os princípios que o diff de fato toca, e não exige interface onde não há fronteira entre camadas.
