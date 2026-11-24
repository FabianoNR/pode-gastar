# 0016: Retrospectiva por card, com script de medição só de leitura

Status: aceita. Abre uma exceção à ADR 0015, seção "Permissões", só para o script de medição.

Data: 2026-11-24.

## Contexto

O fluxo da ADR 0015 grava no GitHub o caminho de cada card: as mudanças de Status, os comentários de decisão, os artefatos, a PR, o CI e o relatório do revisor. Falta olhar para esses dados depois do Done e transformar o que se aprende em ação. A primeira retrospectiva com dados é a do card #31.

O tempo em cada Status só está nos eventos `ProjectV2ItemStatusChangedEvent`, com o nome do Status anterior e do novo. A API REST mostra o evento, mas sem os nomes. Esses eventos só saem pela API GraphQL, e a ADR 0015 nega `gh api graphql` ao agente.

## Decisão

- A retrospectiva é uma skill (`retrospectiva`), chamada pelo Fabiano numa sessão nova depois que o card chega a Done. Nada a dispara sozinho, e o agente não a roda por conta própria. Ela mede por card; a visão do fluxo todo fica para depois.
- A medição fica num script reutilizável, `.github/scripts/retrospectiva-medir.sh <N>`, para um workflow poder rodá-lo no futuro sem refazer.
- **Exceção à ADR 0015:** o script pode chamar `gh api graphql`, só com consultas (`query`). A função que faz a chamada recusa qualquer `mutation` antes de chamar o gh, e um teste confere isso. O deny `Bash(gh api graphql *)` do `.claude/settings.json` continua: o agente não chama o GraphQL direto, só por esse script, e o Claude Code pede permissão para rodá-lo.
- A skill só lê o GitHub: não comenta, não muda Status nem Situação, não cria card. O plano de ação não vira card.
- Os dados ficam fora do repositório, na pasta indicada no `CLAUDE.md`: `retrospectivas.csv` (uma linha por retrospectiva, só acréscimo) e `card-<N>.md`.

## Consequências

- A exceção vale só para o script. Outra necessidade de GraphQL precisa de outra decisão.
- O script mede o que o GitHub guarda. O histórico da Situação (Blocked, Hold) não aparece na linha do tempo e não é medido. Desvios do roteiro e comentários de decisão são contados por heurística e marcados como estimados.
- Riscos conhecidos:
  - A garantia de só leitura é o próprio script (a recusa de `mutation` e o teste). Uma mudança no script passa pela PR e pelo revisor.
  - Os dados fora do repositório não têm backup nem histórico, a não ser que a pasta vire um repositório local.

## Trade-offs

- Liberar `gh api graphql` no settings seria mais simples, mas abriria também as mutations. A exceção por script mantém o deny para o resto.
- Guardar os dados no repositório daria backup e histórico, mas misturaria dados de processo com o código do produto.
