---
name: refinamento
description: Faz o Refinamento de um card que já passou pela Concepção. Use quando o Fabiano indicar um card em Pronto p/ Refinamento (ou em Refinamento, para retomar) e quiser deixá-lo pronto para desenvolver, com critérios conferidos, cenários, seções da spec, decisões técnicas e roteiro.
---

# Refinamento

Etapa do GitHub Project "Pode Gastar" que transforma um card concebido (Pronto p/ Refinamento) num card pronto para desenvolver. A saída é o Status Desenvolvimento: não existe estado "Pronto p/ Desenvolvimento". O fluxo está na ADR 0015.

O resultado fica em dois lugares:

- O corpo do card recebe dois blocos: "## Pronto para desenvolver" (a Definition of Ready, em lista de tarefas) e "## Roteiro de desenvolvimento" (subtarefas em ordem, em lista de tarefas).
- A evidência de cada item, os cenários Gherkin e o trecho da spec proposto vão para um artefato da issue, em Markdown, chamado "Refinamento".

## Princípios

- Português simples. Não use o termo "Job Story".
- UMA pergunta por vez. Espere a resposta antes da próxima.
- Você propõe e o Fabiano decide, nas palavras dele. Nunca preencha lacuna com palpite.
- Toda decisão registra o chapéu (PM, Design, Engenharia, QA etc.).
- Card pequeno não ganha burocracia: item simples tem evidência de uma linha.
- Você é facilitador de conclusão:
  - No início, mostre os 5 itens; a cada passo, diga o que está ok e o que falta.
  - Dúvida que não muda o desenvolvimento não se discute agora: "fora de escopo" ou "decidir no código, com comentário no card".
  - Se o mesmo tópico voltar pela terceira vez, diga que a conversa está girando nele e proponha uma saída: decidir agora, estacionar (anotar e seguir) ou bloquear com Motivo.
- Você NUNCA altera a Situação do card (só o Fabiano muda; você orienta), NUNCA move o card para Done, NUNCA fecha issue e NUNCA faz merge. Só move o Status desta etapa.

## Regras de ferramentas

- Arquivos temporários: ferramenta Write em `/tmp/pode-gastar-card/` (se o arquivo existir, leia com Read antes). Não use `sed`, `tail`, `mktemp` nem `>`.
- Buscas em `docs/` e no código: Grep e Read, não `grep` pelo Bash.
- NUNCA digite IDs do Project de memória nem os fixe neste arquivo. Descubra-os na hora, com os comandos do passo 1, a cada uso.
- Nunca use `gh issue artifact delete`. Para refazer, edite o artefato.
- Se precisar usar `gh api -X PATCH` numa issue (alternativa quando o `gh issue edit` falhar), nunca envie `state`: o PATCH pode fechar a issue.

## Entrada

Receba o número do card. Leia:

- Título e corpo: `gh issue view <N> --json title,body,number`.
- Issue type: `gh api repos/FabianoNR/pode-gastar/issues/<N> --jq .type.name` (pode pedir permissão; o Fabiano aprova na hora).
- Artefatos: `gh issue artifact list <N> --json number,name`, depois `gh issue artifact view <N> <número>` para "Entendimento" e "Concepção".
- As seções da spec listadas em "Seções da spec autorizadas" do corpo, em `docs/spec.md` e, se listado, `docs/operacao.md`.
- As ADRs que tocam o card (liste `docs/decisions/` e leia as relevantes) e o `CLAUDE.md`.

Confira antes de começar (Status e Situação vêm do `gh project item-list`, passo 1):

- O Status é "Pronto p/ Refinamento" ou já "Refinamento" (retomada). Se for outro, pare e avise.
- O corpo tem o bloco "## Concepção" com critérios de aceite e a linha de seções da spec. Se faltar algo, pare e avise.
- O Porte não é Elefante. Se for, pare e avise: elefante não passa adiante.
- A Situação. Normal: siga. Blocked: siga (retomada) e avise o Fabiano de que o card está Blocked; quando a dúvida que bloqueou for respondida na conversa, oriente-o a mudar a Situação para Normal. Hold: pare e avise. Você não altera a Situação.

Retomada: se já existir o artefato "Refinamento", leia-o, parta do que já está decidido, diga ao Fabiano o que foi retomado e pergunte só o que falta.

## Passo a passo

### 1. Status para Refinamento

Só a partir de "Pronto p/ Refinamento"; nunca para trás. Se já estiver em Refinamento, não mexa. Descubra os IDs na hora:

- `gh project list --owner FabianoNR --format json` (id e número do Project "Pode Gastar")
- `gh project field-list <número> --owner FabianoNR --format json` (id do campo "Status" e das opções)
- `gh project item-list <número> --owner FabianoNR --format json --limit 200` (id do item, Status e Situação atuais)
- `gh project item-edit --id <item> --project-id <projeto> --field-id <campo> --single-select-option-id <opção>`

Procure as opções pelo nome exato ("Refinamento", "Desenvolvimento", "Pronto p/ Concepção"). Se um nome não existir, pare e avise. Depois de editar, confira com `gh project item-list` que o Status mudou.

### 2. Blocos no corpo, no começo

Ao começar (não só no fim), crie no corpo os dois blocos, depois do bloco "## Concepção". Retomada: se já existem, não recrie.

```
## Pronto para desenvolver

- [ ] 1. Critérios de aceite revisados
- [ ] 2. Cenários Gherkin garantidos
- [ ] 3. Seções da spec existem
- [ ] 4. Tecnologia e arquitetura vistas
- [ ] 5. Roteiro de desenvolvimento claro

## Roteiro de desenvolvimento

(a preencher no item 5)
```

Aplique as Regras de edição do corpo. A evidência vai para o artefato, não para o corpo.

### 3. Definition of Ready

Um checklist único para todos os tipos de card (versões por tipo ficam para depois do piloto). Trabalhe os itens nesta ordem, um de cada vez.

**Marcação:** só marque `- [x]` um item quando (a) a evidência estiver registrada no artefato e (b) o Fabiano confirmar. Item não ok: trabalhe com ele até ficar ok, sem pular para o próximo.

#### 1. Critérios de aceite revisados

Confira cada critério do bloco "Concepção" com a conta, contra as regras da spec, lendo as regras no `docs/spec.md`. Se o critério estiver errado ou incompleto, proponha o ajuste. O Fabiano decide. Critério ajustado atualiza o bloco "Concepção" do corpo e vira comentário datado no card (ver passo 4). Evidência: cada critério, a conta e o resultado (ok ou ajustado).

#### 2. Cenários Gherkin garantidos

Proponha cenários (Dado, Quando, Então) que cobrem os critérios e algumas bordas. Cenários não vão para a spec: viram testes. Evidência: os cenários, no artefato, cada um ligado ao critério que cobre.

#### 3. Seções da spec listadas existem

Para cada seção da linha "Seções da spec autorizadas", confira com Grep, em `docs/spec.md` (e `docs/operacao.md` quando listado), que o título existe, com o texto exato. Seção ausente ou com outro título: mostre ao Fabiano; ele decide se o card lista a seção certa ou se a seção nova é criada pela PR. Se a regra necessária não está em nenhuma seção, é regra nova: entra na lista (decisão dele). Evidência: cada título e o arquivo onde foi achado.

#### 4. Tecnologia e arquitetura vistas

Leia o código e as ADRs que o card toca (domínio, portas, adaptadores, ADR 0003) e diga o que o card exige em tecnologia e estrutura. Classifique cada decisão:

- **ADR:** só para decisão sobre tecnologia, estrutura ou entrega que seja durável, difícil de desfazer e que outro card ou pessoa precise entender depois (stack, hospedagem, camadas, pipeline, limite técnico, ferramenta de processo). A ADR é escrita na PR do código, não nesta skill.
- **Regra de negócio:** NÃO vira ADR. Vai para a spec (entra no trecho proposto do item 5).
- **Decisão local e fácil de desfazer:** comentário no card (passo 4).

Se não há decisão técnica, marque o item com "sem decisão técnica" e o motivo. Não invente decisão para preencher o item. Evidência: o que foi lido, cada decisão com a classificação, ou "sem decisão técnica" com o motivo.

#### 5. Roteiro de desenvolvimento claro

Proponha:

- As subtarefas em ordem, pequenas, na sequência do CLAUDE.md: testes dos critérios primeiro (e vê-los falhar), depois a implementação, depois as seções da spec e de `docs/operacao.md` que o card lista.
- O trecho da spec proposto, como diff, só nas seções autorizadas. É proposta: a spec só muda na PR do código.

O Fabiano ajusta e confirma. Evidência: o trecho da spec proposto no artefato. As subtarefas ficam no bloco "## Roteiro de desenvolvimento" do corpo, como lista de tarefas `- [ ]`.

### 4. Decisões e dúvidas

Cada decisão tomada vira um comentário no card, um por decisão, com: data, decisão, chapéu e motivo. Comando: `gh issue comment <N> --body-file /tmp/pode-gastar-card/<N>-decisao-<n>.md`. Postar depois da confirmação do Fabiano.

Dúvida que muda o comportamento visível do sistema:

- **Cabe no mesmo card**, sem mudar o problema, a solução nem o porte: resolva aqui. Pergunte, o Fabiano decide, os critérios do bloco "Concepção" são atualizados (Regras de edição do corpo) e um comentário datado vai no card.
- **Muda o problema ou a solução, ou o card cresce para elefante:** pare. Explique o motivo. O Fabiano decide: volta à Concepção (conta como retrabalho) ou vira card novo. Se ele mandar voltar, grave o artefato (o que já foi decidido fica salvo) e mova o Status para "Pronto p/ Concepção" (fila; a skill de Concepção move para ativo ao começar). É a exceção à regra "nunca para trás", só por decisão dele. Card novo: só crie depois da decisão dele.

Dúvida que depende de pessoa ou de teste real: o card fica em Refinamento. Grave o artefato com o que já foi decidido e diga ao Fabiano: "mude a Situação para Blocked com este Motivo: <pergunta e quem responde>". Ele executa. Você não altera a Situação. Pare.

### 5. Saída

Com os 5 itens marcados no corpo e a confirmação do Fabiano:

1. Mostre o resumo: decisões, decisões técnicas e roteiro.
2. Depois da confirmação, mova o Status para "Desenvolvimento", com os IDs descobertos agora (passo 1). Confira o nome exato da opção no `gh project field-list`.
3. Diga o que foi feito e o que ficou para o Fabiano (Situação, ADR a escrever na PR).

### 6. Pedidos de ajuste depois do Refinamento

- Enquanto o Status não for Desenvolvimento: o card é ajustado, com comentário datado.
- Depois que o Status é Desenvolvimento, quem manda é a spec da PR: o ajuste entra nela e o card recebe só um comentário datado (data, ajuste, chapéu e motivo).

## Artefato "Refinamento"

Escreva com Write em `/tmp/pode-gastar-card/<N>-refinamento.md`. Campo sem conteúdo fica com "não se aplica".

```
# Refinamento

## 1. Critérios de aceite revisados

**Critérios e contas:** critério, conta e resultado (ok ou ajustado)

## 2. Cenários Gherkin

**Cenários:** Dado, Quando, Então, com o critério que cobre

## 3. Seções da spec

**Seções conferidas:** título exato e arquivo

## 4. Tecnologia e arquitetura

**Lido:** código e ADRs

**Decisões:** classificação (ADR, spec ou comentário) e chapéu; ou "sem decisão técnica" e o motivo

## 5. Roteiro

**Trecho da spec proposto:**

(diff)

## Dúvidas e destino

**Dúvidas:** com o destino (resolvida aqui, fora de escopo, voltou à Concepção, Blocked)

## Decisões do Fabiano

**Decisões:** data, decisão, chapéu e motivo
```

## Regras de edição do corpo

O corpo é um campo único e a edição grava o corpo inteiro. Só são desta skill os blocos "## Pronto para desenvolver" e "## Roteiro de desenvolvimento", mais o ajuste dos critérios e da linha "Seções da spec autorizadas" do bloco "## Concepção" quando o Fabiano decide. Portanto:

1. Leia o corpo atual: `gh issue view <N> --json body --jq .body`. Com a ferramenta Write, grave a cópia exata em `/tmp/pode-gastar-card/<N>-antigo.md`.
2. Nunca altere o que não é da skill (texto da criação, "Resumo do Entendimento", o resto da "Concepção"). Acrescente os blocos depois do bloco "## Concepção", ou substitua só o próprio bloco (do título até o próximo "## " ou o fim do corpo).
3. Escreva o corpo novo com a ferramenta Write em `/tmp/pode-gastar-card/<N>-novo.md`. Antes de gravar, compare: `diff /tmp/pode-gastar-card/<N>-antigo.md /tmp/pode-gastar-card/<N>-novo.md`. O diff só pode mostrar acréscimos ou mudanças nos próprios blocos (e, quando o Fabiano decidiu, nas linhas do critério ajustado ou da linha "Seções da spec autorizadas"). Se mostrar outra coisa, pare e não grave.
4. Grave o corpo inteiro de volta: `gh issue edit <N> --body-file /tmp/pode-gastar-card/<N>-novo.md`.
5. Marcar um item também é uma edição do corpo: repita 1 a 4.

## Gravar o artefato

1. Veja se o artefato já existe: `gh issue artifact list <N> --json number,name`. Procure `"name": "Refinamento"`.
2. Se não existir, crie: `gh issue artifact create <N> /tmp/pode-gastar-card/<N>-refinamento.md#Refinamento`. O número do artefato vem na saída.
3. Se existir, substitua: `gh issue artifact edit <N> <número-do-artefato> --body-file /tmp/pode-gastar-card/<N>-refinamento.md`. O `edit` substitui o conteúdo inteiro: o arquivo novo traz tudo que já estava decidido.
4. Confira: `gh issue artifact view <N> <número-do-artefato>`.

Grave o artefato a cada item confirmado, antes de marcar o item no corpo.
