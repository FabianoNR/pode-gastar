---
name: entendimento
description: Faz o Entendimento (chapéu PM) de um pedido. Use quando o Fabiano trouxer um pedido solto, colado de qualquer canal (WhatsApp, e-mail etc.), ou indicar um card em Backlog para fazer o Entendimento.
---

# Entendimento

Etapa do GitHub Project "Pode Gastar" que transforma um pedido em um card entendido, com decisão do Fabiano: Seguir, Hold ou Descartar. O fluxo está na ADR 0015.

## Regras gerais

- Português simples.
- UMA pergunta por vez. Espere a resposta antes da próxima.
- Você propõe e o Fabiano decide. As respostas finais são dele, nas palavras dele.
- Sem desenho de solução. Se o pedido já traz uma solução, guarde-a no campo "Solução sugerida no pedido" para a Concepção.
- Não use o termo "Job Story".
- Nunca preencha lacuna com palpite.
- Você NUNCA altera a Situação do card e NUNCA fecha issue.

## Entradas

### (a) Texto solto

1. Proponha o tipo (Story, Bug ou Dívida) e espere o Fabiano confirmar.
2. Se houver vários pedidos no texto, proponha um card por pedido e o Fabiano decide um por vez.
3. Distribua o texto nos campos do template do tipo (`.github/ISSUE_TEMPLATE/story.md`, `bug.md` ou `divida.md`), sem inventar. O que o texto não responde vira lacuna: pergunte, uma por vez.
4. Coloque o canal e a data em "Contexto" (ex.: "veio por WhatsApp em 22/11").
5. NÃO copie o texto original para o card: só um resumo. O original só entra se o Fabiano confirmar que não tem dado pessoal nem segredo.
6. Mostre o corpo montado e só crie depois da confirmação. Monte o corpo a partir do arquivo do template, sem o front matter (do segundo `---` em diante), num arquivo temporário. Crie a issue:
   `gh issue create --title "<título>" --body-file <arquivo>`
7. Aplique o Issue type (o gh 2.45 não tem `--type`). O valor é Story, Bug ou Dívida:
   `gh api -X PATCH repos/FabianoNR/pode-gastar/issues/<N> -f type=<tipo>`
   Confira na resposta que o campo `type` voltou com o nome certo. O PATCH em issue pode fechar a issue se `state` for enviado: nunca envie `state`.
8. A issue entra sozinha no Project em Backlog, pela automação. Confirme com `gh project item-list` antes de mover.

### (b) Card existente

Receba o número. Leia título, corpo e Issue type:
`gh issue view <N> --json title,body,number`
`gh api repos/FabianoNR/pode-gastar/issues/<N> --jq .type.name`

## Passo a passo

### 1. Status para Entendimento

Só a partir de Backlog; nunca para trás. Descubra IDs em tempo de execução, sem fixar IDs neste arquivo:

- `gh project list --owner FabianoNR --format json` (id e número do Project "Pode Gastar")
- `gh project field-list <número> --owner FabianoNR --format json` (id do campo e das opções de Status e Prioridade)
- `gh project item-list <número> --owner FabianoNR --format json --limit 200` (id do item e Status atual do card)
- `gh project item-edit --id <item> --project-id <projeto> --field-id <campo> --single-select-option-id <opção>`

Se o Status atual não for Backlog (e não for já Entendimento), pare e avise.

### 2. Situação

Se a Situação do card não for Normal (os cards #31 a #49 estão em Hold), avise que ela só pode ser mudada pelo Fabiano e peça que ele mude para Normal quando decidir Seguir. Não a altere.

### 3. As 8 perguntas

Uma por vez. Para cada uma, leia o que já está no corpo e julgue se responde de forma satisfatória:

- Se sim: apresente a pergunta já com a resposta e a fonte ("do seu texto" ou "da spec/ADR/card"). O Fabiano confirma ou muda.
- Se há dúvida: traga a dúvida e pergunte.
- Se não há resposta: pergunte e espere.

1. Qual é o problema real por trás do pedido?
2. Em que situação ele aparece e que evidência temos de que é real? Quando aconteceu, quantas vezes?
3. O produto já resolve, mesmo de outro jeito? Pesquise por palavras-chave do corpo em `docs/spec.md`, `docs/operacao.md`, `docs/decisions/` (com grep) e nas issues, abertas e fechadas como "not planned". Relate o que cobre e o que não cobre. Sintaxe que funciona no gh 2.45:
   - `gh issue list --state all --search "<palavra> in:title,body" --json number,title,state`
   - `gh issue list --state closed --search "reason:\"not planned\" <palavra>" --json number,title`
   (`--json stateReason` não existe no gh 2.45. Pesquise uma palavra por vez.)
4. Impacto para o produto: o que muda, ou o que se perde, para quem usa? Vale para todos os tipos, inclusive Dívida (o impacto de NÃO resolver conta).
5. Impacto para o negócio: como isso afeta o controle financeiro como produto real, para o Fabiano e para outras pessoas.
6. Faz sentido fazer agora? Pelo momento, pelo custo de manter e pelo que deixamos de fazer para fazer isso.
7. Como saberemos que deu certo? OPCIONAL por ora: pergunte uma vez. Se o Fabiano não tiver, registre "sem métrica definida".
8. O que acontece se esperar? Normalmente já vem do campo do template.

### 4. Bloco montado

Mostre o bloco de Entendimento com as 8 respostas para o Fabiano confirmar. Os campos Decisão, Prioridade e Motivo vêm do passo 5: o bloco só é gravado depois da decisão, uma vez só, com o bloco inteiro confirmado pelo Fabiano.

Formato, acrescentado ao final do corpo:

```
## Entendimento (chapéu PM)
**Solução sugerida no pedido:** [se o pedido já trouxe uma solução, guardada para a Concepção; senão "nenhuma"]
**Problema real:** ...
**Situação e evidência:** ...
**O produto já resolve?** ...
**Impacto no produto:** ...
**Impacto no negócio:** ...
**Faz sentido agora?** ...
**Como saberemos que deu certo:** ... (ou "sem métrica definida")
**Se esperar:** ...
**Decisão:** Seguir | Hold | Descartar
**Prioridade:** alta | média | baixa — justificativa em uma linha (só se Seguir)
**Motivo:** ... (só se Hold ou Descartar)
```

### Regras de edição do corpo

O corpo é um campo único. Portanto:

1. Leia o corpo atual: `gh issue view <N> --json body --jq .body > <arquivo>`.
2. Nunca altere a parte escrita na criação. Só acrescente o bloco ou substitua o próprio bloco (do título "## Entendimento" até o próximo "## " de outra etapa, ou o fim do corpo). Se o bloco já existir (Entendimento refeito), substitua só ele.
3. Antes de gravar, compare o novo corpo com o antigo (`diff`): tudo fora do bloco tem de estar idêntico. Se não estiver, pare e não grave.
4. Grave o corpo inteiro de volta: `gh issue edit <N> --body-file <arquivo>`.

### 5. Decisão do Fabiano

Pergunte a decisão. Complete o bloco com Decisão e com Prioridade ou Motivo, mostre o bloco inteiro e grave o corpo depois da confirmação (Regras de edição do corpo). Só depois mova o Status ou poste o comentário.

- **Seguir:** pergunte a Prioridade (alta, média, baixa) e uma linha de justificativa. Grave a Prioridade no campo do Project (`gh project item-edit`, opção Alta/Média/Baixa descoberta no field-list) e mova o Status para "Pronto p/ Concepção". Se a Situação não for Normal, lembre o Fabiano de mudá-la.
- **Hold:** pergunte o Motivo e poste como comentário no card (`gh issue comment <N> --body ...`). Diga: "mude a Situação para Hold com este Motivo". Não altere a Situação.
- **Descartar:** pergunte o Motivo e poste como comentário. Diga ao Fabiano para fechar a issue como "not planned". Não feche a issue.

Depois de gravar, diga o que foi feito e o que ficou para o Fabiano.
