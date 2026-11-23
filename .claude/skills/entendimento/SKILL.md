---
name: entendimento
description: Faz o Entendimento (chapéu PM) de um pedido. Use quando o Fabiano trouxer um pedido solto, colado de qualquer canal (WhatsApp, e-mail etc.), ou indicar um card em Backlog para fazer o Entendimento.
---

# Entendimento

Etapa do GitHub Project "Pode Gastar" que transforma um pedido em um card entendido, com decisão do Fabiano: Seguir, Hold ou Descartar. O fluxo está na ADR 0015.

O resultado fica em dois lugares:

- O Entendimento completo vira um artefato da issue, em Markdown, chamado "Entendimento".
- O corpo do card recebe só um bloco curto, "Resumo do Entendimento", no final.

## Regras gerais

- Português simples.
- UMA pergunta por vez. Espere a resposta antes da próxima.
- Você propõe e o Fabiano decide. As respostas finais são dele, nas palavras dele.
- Sem desenho de solução. Se o pedido já traz uma solução, guarde-a no campo "Solução sugerida no pedido" para a Concepção.
- Não use o termo "Job Story".
- Nunca preencha lacuna com palpite.
- Você NUNCA altera a Situação do card e NUNCA fecha issue.
- Nunca use `gh issue artifact delete`. Para refazer, edite o artefato: o GitHub guarda as versões.
- Arquivos temporários: use a ferramenta Write, num diretório fora do repositório (`/tmp/pode-gastar-card/`). Não use `sed`, `tail`, `mktemp` nem redirecionamento `>`.
- Buscas em `docs/`: use a ferramenta Grep, não `grep` pelo Bash.

## Entradas

### (a) Texto solto

1. Proponha o tipo (Story, Bug ou Dívida) e, se o campo "Fonte da Demanda" existir no Project (passo 3), proponha também a Fonte da Demanda. Espere o Fabiano confirmar os dois.
2. Se houver vários pedidos no texto, proponha um card por pedido e o Fabiano decide um por vez.
3. Distribua o texto nos campos do template do tipo (`.github/ISSUE_TEMPLATE/story.md`, `bug.md` ou `divida.md`), sem inventar. O que o texto não responde vira lacuna: pergunte, uma por vez.
4. Coloque o canal e a data em "Contexto" (ex.: "veio por WhatsApp em 22/11").
5. NÃO copie o texto original para o card: só um resumo. O original só entra se o Fabiano confirmar que não tem dado pessoal nem segredo.
6. Mostre o corpo montado e só crie depois da confirmação. Leia o template com a ferramenta Read e escreva com a ferramenta Write o corpo montado, sem o front matter (do segundo `---` em diante), em `/tmp/pode-gastar-card/<slug>-corpo.md`. Crie a issue:
   `gh issue create --title "<título>" --body-file /tmp/pode-gastar-card/<slug>-corpo.md`
7. Aplique o Issue type (o gh 2.45 não tem `--type`). O valor é Story, Bug ou Dívida:
   `gh api -X PATCH repos/FabianoNR/pode-gastar/issues/<N> -f type=<tipo>`
   Confira na resposta que o campo `type` voltou com o nome certo. O PATCH em issue pode fechar a issue se `state` for enviado: nunca envie `state`.
8. A issue entra sozinha no Project em Backlog, pela automação. Confirme com `gh project item-list` antes de mover. Grave a Fonte da Demanda confirmada no passo 1 (passo 3 do Passo a passo).

### (b) Card existente

Receba o número. Leia título, corpo e Issue type:
`gh issue view <N> --json title,body,number`
`gh api repos/FabianoNR/pode-gastar/issues/<N> --jq .type.name`
Esse comando pode pedir permissão; o Fabiano aprova na hora.

O tipo vem do Issue type. Se ele estiver vazio, pare e pergunte o tipo ao Fabiano.

## Passo a passo

### 1. Status para Entendimento

Só a partir de Backlog; nunca para trás. A única exceção é a decisão Hold (passo 6), que devolve o Status para Backlog. Descubra IDs em tempo de execução, sem fixar IDs neste arquivo:

- `gh project list --owner FabianoNR --format json` (id e número do Project "Pode Gastar")
- `gh project field-list <número> --owner FabianoNR --format json` (id do campo e das opções de Status, Prioridade e Fonte da Demanda)
- `gh project item-list <número> --owner FabianoNR --format json --limit 200` (id do item, Status atual e valores dos campos do card)
- `gh project item-edit --id <item> --project-id <projeto> --field-id <campo> --single-select-option-id <opção>`

Se o Status atual não for Backlog (e não for já Entendimento), pare e avise.

### 2. Situação

Se a Situação do card não for Normal (os cards #31 a #49 estão em Hold), avise que ela só pode ser mudada pelo Fabiano e peça que ele mude para Normal quando decidir Seguir. Não a altere.

### 3. Fonte da Demanda

Procure o campo pelo nome "Fonte da Demanda" no `gh project field-list` e leia as opções de lá. Não fixe IDs nem opções neste arquivo.

- Se o campo não existir: ignore este passo e avise o Fabiano que o campo não foi encontrado.
- Texto solto: o valor foi proposto e confirmado junto com o tipo (entrada (a), passo 1). Grave-o com `gh project item-edit` depois que a issue entrar no Project.
- Card existente: no `gh project item-list`, o valor aparece na chave `"fonte da Demanda"` do item; se a chave não aparecer, o campo está vazio. Se estiver vazio, pergunte uma vez, proponha um valor entre as opções, e grave depois da confirmação. Se já tiver valor, não mexa.

### 4. As perguntas, por tipo

O conjunto depende do tipo (Issue type). Uma pergunta por vez. Para cada uma, leia o que já está no corpo e julgue se responde de forma satisfatória:

- Se sim: apresente a pergunta já com a resposta e a fonte ("do seu texto" ou "da spec/ADR/card"). O Fabiano confirma ou muda.
- Se há dúvida: traga a dúvida e pergunte.
- Se não há resposta: pergunte e espere.

Pesquisa da pergunta "O produto já resolve" (vale para todos os tipos): pesquise por palavras-chave do corpo em `docs/spec.md`, `docs/operacao.md`, `docs/decisions/` (com a ferramenta Grep) e nas issues, abertas e fechadas como "not planned". Relate o que cobre e o que não cobre. Sintaxe que funciona no gh 2.45:

- `gh issue list --state all --search "<palavra> in:title,body" --json number,title,state`
- `gh issue list --state closed --search "reason:\"not planned\" <palavra>" --json number,title`

(`--json stateReason` não existe no gh 2.45. Pesquise uma palavra por vez.)

#### Story: 8 perguntas

1. Qual é o problema real por trás do pedido?
2. Em que situação ele aparece e que evidência temos de que é real? Quando aconteceu, quantas vezes?
3. O produto já resolve, mesmo de outro jeito? Já existe card ou ADR sobre isso? (pesquisa acima)
4. Impacto para o produto: o que muda, ou o que se perde, para quem usa?
5. Impacto para o negócio: como isso afeta o controle financeiro como produto real, para o Fabiano e para outras pessoas.
6. Faz sentido fazer agora? Pelo momento, pelo custo de manter e pelo que deixamos de fazer para fazer isso.
7. Como saberemos que deu certo? OPCIONAL por ora: pergunte uma vez. Se o Fabiano não tiver, registre "sem métrica definida".
8. O que acontece se esperar? Normalmente já vem do campo do template.

#### Bug e Dívida: 6 perguntas

1. Qual é o problema real?
2. Onde ele aparece e que evidência temos de que é real? Quando aconteceu, quantas vezes?
3. O produto já resolve, mesmo de outro jeito? Já existe card ou ADR sobre isso? (pesquisa acima)
4. O que se perde se esperar, para o produto (quem usa) e para o negócio? O impacto de NÃO resolver conta. Parte da resposta normalmente vem do campo "o que acontece se isso esperar" do template.
5. Faz sentido fazer agora? Pelo momento, pelo custo de manter e pelo que deixamos de fazer para fazer isso.
6. Como saberemos que deu certo? OPCIONAL por ora: pergunte uma vez. Se o Fabiano não tiver, registre "sem métrica definida".

### 5. Artefato e resumo montados

Monte os dois textos com as respostas confirmadas. Os campos de decisão vêm do passo 6: nada é gravado antes da decisão.

#### Artefato "Entendimento"

Escreva com a ferramenta Write em `/tmp/pode-gastar-card/<N>-entendimento.md`. Use os campos do tipo:

```
# Entendimento (chapéu PM)

**Tipo:** Story | Bug | Dívida
**Solução sugerida no pedido:** [se o pedido já trouxe uma solução, guardada para a Concepção; senão "nenhuma"]

## Respostas

(Story)
**Problema real:** ...
**Situação e evidência:** ...
**O produto já resolve?** ...
**Impacto no produto:** ...
**Impacto no negócio:** ...
**Faz sentido agora?** ...
**Como saberemos que deu certo:** ... (ou "sem métrica definida")
**Se esperar:** ...

(Bug e Dívida)
**Problema real:** ...
**Onde aparece e evidência:** ...
**O produto já resolve?** ...
**O que se perde se esperar (produto e negócio):** ...
**Faz sentido agora?** ...
**Como saberemos que deu certo:** ... (ou "sem métrica definida")

## Decisão

**Decisão:** Seguir | Hold | Descartar
**Prioridade:** alta | média | baixa — justificativa em uma linha (só se Seguir)
**Motivo:** ... (só se Hold ou Descartar)
```

Grave só o bloco do tipo do card, sem as linhas "(Story)" e "(Bug e Dívida)".

#### Resumo no corpo

Bloco curto, no final do corpo, depois do texto da criação:

```
## Resumo do Entendimento
**O problema:** [linguagem clara, 1 ou 2 frases]
**O que se ganha resolvendo:** [1 ou 2 frases]
**Decisão:** Seguir | Hold | Descartar — Prioridade (se Seguir) e justificativa em uma linha; ou o Motivo
**Detalhe:** artefato "Entendimento" desta issue
```

### Regras de edição do corpo

O corpo é um campo único e só o bloco "Resumo do Entendimento" é da skill. Portanto:

1. Leia o corpo atual: `gh issue view <N> --json body --jq .body` (a saída vem para a conversa). Com a ferramenta Write, grave a cópia exata em `/tmp/pode-gastar-card/<N>-antigo.md`.
2. Nunca altere a parte escrita na criação. Só acrescente o bloco no final ou substitua o próprio bloco (do título "## Resumo do Entendimento" até o próximo "## " ou o fim do corpo). Se o bloco já existir (Entendimento refeito), substitua só ele.
3. Escreva o corpo novo com a ferramenta Write em `/tmp/pode-gastar-card/<N>-novo.md`. Antes de gravar, compare: `diff /tmp/pode-gastar-card/<N>-antigo.md /tmp/pode-gastar-card/<N>-novo.md`. Tudo fora do bloco tem de estar idêntico. Se não estiver, pare e não grave.
4. Grave o corpo inteiro de volta: `gh issue edit <N> --body-file /tmp/pode-gastar-card/<N>-novo.md`.

### Gravar o artefato

1. Veja se o artefato já existe:
   `gh issue artifact list <N> --json number,name`
   A saída é uma lista JSON (`[]` se a issue não tem artefato). Procure o item com `"name": "Entendimento"`.
2. Se não existir, crie (o `#Entendimento` dá o nome ao artefato):
   `gh issue artifact create <N> /tmp/pode-gastar-card/<N>-entendimento.md#Entendimento`
3. Se já existir, substitua o conteúdo, usando o `number` do artefato:
   `gh issue artifact edit <N> <número-do-artefato> --body-file /tmp/pode-gastar-card/<N>-entendimento.md`
4. Confira: `gh issue artifact view <N> <número-do-artefato>`.

A verificar no piloto: a saída do `create` (se traz o número ou a URL do artefato) e se o `edit` guarda a versão anterior no histórico, como diz o `--help`.

### 6. Decisão do Fabiano

Pergunte a decisão:

- **Seguir:** pergunte a Prioridade (alta, média, baixa) e uma linha de justificativa.
- **Hold** ou **Descartar:** pergunte o Motivo.

Complete o artefato com Decisão e com Prioridade ou Motivo, e o resumo com a linha de Decisão. Mostre os dois ao Fabiano. Depois da confirmação, nesta ordem:

1. Grave o artefato (Gravar o artefato).
2. Grave o corpo (Regras de edição do corpo).
3. Só então mova o Status, poste o comentário ou grave a Prioridade, conforme a decisão:

- **Seguir:** grave a Prioridade no campo do Project (`gh project item-edit`, opção Alta/Média/Baixa descoberta no field-list) e mova o Status para "Pronto p/ Concepção". Se a Situação não for Normal, lembre o Fabiano de mudá-la.
- **Hold:** poste o Motivo como comentário no card (`gh issue comment <N> --body ...`). Depois do comentário, mova o Status para Backlog (`gh project item-edit`, opção Backlog descoberta no field-list). Esta é a exceção explícita à regra "nunca para trás" do passo 1: Entendimento é estado ativo, e um card parado em Hold não pode contar como tempo ativo. Diga: "mude a Situação para Hold com este Motivo". Não altere a Situação.
- **Descartar:** poste o Motivo como comentário. Diga ao Fabiano para fechar a issue como "not planned". Não feche a issue. O Status não muda: quando o Fabiano fecha a issue, a automação manda o card para Done.

Depois de gravar, diga o que foi feito e o que ficou para o Fabiano.
