---
name: concepcao
description: Faz a Concepção de um card que já passou pelo Entendimento. Use quando o Fabiano indicar um card em Pronto p/ Concepção (ou em Concepção, para retomar) e quiser definir a solução, os critérios de aceite, o porte, os riscos e as seções da spec.
---

# Concepção

Etapa do GitHub Project "Pode Gastar" que transforma um card entendido (decisão Seguir) num card pronto para refinar: solução definida, critérios de aceite, porte, riscos e seções da spec autorizadas. O fluxo está na ADR 0015.

O resultado fica em dois lugares:

- O detalhe da Concepção (CSD, investigações, riscos, INVEST, fatiamento, decisões) vira um artefato da issue, em Markdown, chamado "Concepção".
- O corpo do card recebe um bloco "## Concepção", depois do "## Resumo do Entendimento", com o que o downstream usa: solução, critérios de aceite, porte, riscos que afetam o agente, seções da spec autorizadas e cenários.

## Princípios

- Português simples. Não use o termo "Job Story".
- UMA pergunta por vez. Espere a resposta antes da próxima.
- Você propõe e o Fabiano decide. As respostas finais são dele, nas palavras dele.
- Nunca preencha lacuna com palpite. O que faltar vira pergunta.
- O rigor é proporcional ao tamanho do card: em card pequeno, o CSD pode ter poucos itens.
- Você é facilitador de conclusão:
  - No início, mostre o que é preciso para concluir: solução, critérios de aceite, porte, riscos e seções da spec.
  - A cada passo, diga o que já foi coberto e o que falta.
  - Dúvida que não é crítica não se discute agora: vai para "respondida no Refinamento" ou para "fora de escopo".
  - Se o mesmo tópico voltar pela terceira vez, diga que a conversa está girando nele e proponha uma saída: decidir agora, estacionar (anotar e seguir) ou bloquear com Motivo.
- Você NUNCA altera a Situação do card e NUNCA fecha issue. Só move o Status desta etapa.

## Regras de ferramentas

- Arquivos temporários: use a ferramenta Write em `/tmp/pode-gastar-card/`. Se o arquivo já existir, leia-o com a ferramenta Read antes de sobrescrever. Não use `sed`, `tail`, `mktemp` nem redirecionamento `>`.
- Buscas em `docs/`: use a ferramenta Grep, não `grep` pelo Bash. Leitura de código: ferramentas Read e Grep.
- Não fixe IDs do Project neste arquivo: descubra-os em tempo de execução (passo 1).
- Nunca use `gh issue artifact delete`. Para refazer, edite o artefato.
- O `gh` instalado é o 2.105.0. Ele tem `--type` em `gh issue create` e em `gh issue edit`. Se precisar usar `gh api -X PATCH` numa issue (alternativa quando o `gh issue edit` falhar), nunca envie `state`: o PATCH pode fechar a issue.

## Entrada

Receba o número do card. Leia título, corpo e Issue type:

- `gh issue view <N> --json title,body,number`
- `gh api repos/FabianoNR/pode-gastar/issues/<N> --jq .type.name` (pode pedir permissão; o Fabiano aprova na hora)

Leia o artefato "Entendimento":

- `gh issue artifact list <N> --json number,name` (procure `"name": "Entendimento"`)
- `gh issue artifact view <N> <número-do-artefato>`

Retomada: na mesma lista, veja se já existe um artefato "Concepção" (`"name": "Concepção"`). Se existir, leia-o com `gh issue artifact view <N> <número-do-artefato>` e parta do que já está decidido. Pergunte só o que falta e diga ao Fabiano o que foi retomado.

Confira, antes de começar (Status e Situação vêm do `gh project item-list`, com os comandos do passo 1):

- O Status é "Pronto p/ Concepção" (ou já "Concepção", quando for retomada). Se for outro, pare e avise.
- O artefato "Entendimento" existe e traz a decisão Seguir. Se não, pare e avise.
- O Issue type está preenchido. Se estiver vazio, pare e pergunte o tipo.
- A Situação. Se não for Normal, avise que só o Fabiano pode mudá-la. Não a altere.

Leia também o campo "Solução sugerida no pedido" do artefato: é o ponto de partida da solução (passo 3b), não a solução decidida.

## Passo a passo

### 1. Status para Concepção

Só a partir de "Pronto p/ Concepção"; nunca para trás. Se já estiver em Concepção, não mexa. Descubra os IDs em tempo de execução:

- `gh project list --owner FabianoNR --format json` (id e número do Project "Pode Gastar")
- `gh project field-list <número> --owner FabianoNR --format json` (id do campo Status e das opções)
- `gh project item-list <número> --owner FabianoNR --format json --limit 200` (id do item, Status e Situação atuais)
- `gh project item-edit --id <item> --project-id <projeto> --field-id <campo> --single-select-option-id <opção>`

### 2. Modo

Leia o problema e a "Solução sugerida no pedido" e proponha um modo, com uma linha de justificativa. Espere o Fabiano confirmar.

- **Modo card (fatiar):** o problema está claro e há uma solução viável.
- **Modo descoberta:** o problema está nebuloso, é um elefante, há muitas questões em aberto ou várias soluções possíveis.

Mostre então o que falta para concluir (solução, critérios de aceite, porte, riscos, seções da spec) e comece.

### 3. Modo card

Siga esta ordem. Ao fim de cada item, diga o que foi coberto e o que falta.

#### a) CSD (certezas, suposições e dúvidas)

Proponha itens a partir do corpo, do artefato "Entendimento", da spec e do código. O Fabiano classifica cada um em certeza, suposição ou dúvida. O foco é não tratar suposição como certeza e deixar as dúvidas visíveis.

Para cada dúvida, pergunte (uma por vez):

1. "Se a resposta for o oposto, a solução muda?" Se não muda, a dúvida não é crítica.
2. "Isso precisa ser respondido agora ou tem momento certo mais adiante?"

Destino de cada dúvida:

- **Não crítica:** "respondida no Refinamento" ou "fora de escopo".
- **Crítica, técnica** (impacto no código, viabilidade, POC descartável): investigue lendo o código com Read e Grep, dentro da Concepção. Registre o resultado em "Investigações feitas" no artefato. POC descartável não entra no repositório. Não existe tipo Spike.
- **Crítica, que depende de pessoa, de teste real ou de outra área:** o Status continua em Concepção. Diga ao Fabiano: "mude a Situação para Blocked com este Motivo: ...". Ele executa. Grave o que já foi decidido no artefato (Gravar o artefato) e pare.
- **Investigação grande demais para a Concepção:** pergunte ao Fabiano se vira card filho. Não crie sem a decisão dele.

#### b) Solução definida

Na forma que o tipo de mudança pede:

- **API:** o contrato. Rota, parâmetros, códigos de resposta e formato da resposta, com o que muda em relação à seção "Contrato da API" de `docs/spec.md` e às ADRs 0005 (base da consulta: mês no servidor, GET, recurso `avaliacao`) e 0017 (parâmetro `intensidade`, que substitui em parte a forma da consulta da 0005).
- **Tela** (hoje não há): protótipo ou briefing de Design. Se aparecer, pergunte ao Fabiano como ele quer o protótipo ou o briefing (a definir).
- **Sem mudança visível** (Dívida, operação): o que muda e onde, em uma ou duas frases.

Leia a spec e, se tocar deploy ou scripts, `docs/operacao.md`, para saber o que o sistema faz hoje.

#### c) Riscos e mitigação

Para cada risco, uma mitigação. Cubra:

- usabilidade (quem usa entende e consegue usar?);
- viabilidade técnica (o código e a arquitetura suportam? ADRs que tocam);
- alinhamento com outras áreas (algo depende de alguém de fora?).

Mitigação que vira trabalho do agente também vira critério de aceite (item e).

#### d) INVEST em cada fatia

Checklist, fatia por fatia (o card inteiro é uma fatia se não houver fatiamento):

- **Independente:** pode ser entregue sem esperar outra fatia?
- **Negociável:** o como ainda pode ser ajustado no Refinamento?
- **Valiosa:** gera valor para quem usa OU é mitigação declarada de risco. No segundo caso, anote que a fatia entra atrás de feature toggle. A decisão sobre o toggle fica para o primeiro elefante: não decida agora.
- **Estimável:** dá para dizer o porte?
- **Pequena:** cabe numa PR?
- **Testável:** os critérios de aceite são verificáveis?

Item que falha: mostre ao Fabiano e pergunte o que fazer.

#### e) Critérios de aceite

Determinísticos, com exemplo de entrada e saída. Exemplo de forma (não de conteúdo):

```
CA: com orçamento X e gasto Y, GET /meses/{mesId}/avaliacao?valor=Z&categoria=W
responde 200 com o corpo "..." (só os campos que a spec define para esse caso)
```

Valores monetários com duas casas decimais, como nos exemplos da spec (ADR 0002: BigDecimal). Se um critério depende de uma regra que não está na spec, ela entra como regra nova na lista de seções autorizadas (item i).

#### f) Cenários (desejável)

Proponha alguns cenários Gherkin (Dado, Quando, Então) a partir dos critérios. São desejáveis, não obrigatórios: quem garante que existam é o Refinamento. Se o Fabiano não quiser, siga.

#### g) Fora de escopo

O que não resolve o problema é candidato a fora de escopo. Junte aqui as dúvidas não críticas mandadas para cá no item a.

#### h) Porte: rato, cachorro ou elefante

Proponha o porte com uma linha de justificativa. O Fabiano decide.

Rato e cachorro: o Porte é gravado no campo "Porte" do Project no passo 5, junto com o Status. Confira já aqui, no `gh project field-list`, que o campo tem as opções Rato e Cachorro. Se as opções forem diferentes, pare e avise o Fabiano. Elefante: não grave nada; o campo não tem essa opção e o card não sai da Concepção.

- **Rato:** pergunte "isso gera valor para quem usa (Story), é tarefa de outra entrega, ou é Dívida tratada como Story?". O Fabiano decide. Se for Dívida, ele troca o Issue type (você não troca). Se for tarefa de outra entrega, o destino do card é dele (a definir).
- **Cachorro:** segue.
- **Elefante:** é sinal de quebrar. Elefante nunca passa adiante.
  1. Proponha o fatiamento: a menor parte que já agrega resultado, depois a seguinte.
  2. Aplique o INVEST (item d) em cada fatia.
  3. Monte o sequenciador: ordem das fatias e dependências entre elas.
  4. Decidir no primeiro elefante: o destino do card original e o Status inicial dos cards novos. Você propõe e pergunta ao Fabiano; não decida. Cards novos só são criados depois da decisão dele (o `gh` 2.105.0 aceita `gh issue create --type <tipo> --parent <N>`).

#### i) Seções da spec autorizadas

UMA linha, com arquivo e seção, só o que o card muda. Exemplo: "spec.md: Regras de decisão e Critérios de aceite". Confira os nomes das seções com Grep em `docs/spec.md` e `docs/operacao.md`. Esta linha é a autorização do downstream para mudar a spec (ADR 0015).

### 4. Modo descoberta

Leia `docs/produto.md` (visão e personas). Se o arquivo não existir, avise o Fabiano e siga sem ele.

Use técnicas de descoberta conforme o caso: visão, personas, brainstorm e outras que o Fabiano escolher. Proponha a técnica com uma linha de justificativa e pergunte como ele quer conduzi-la. Não invente o procedimento da técnica: (a detalhar depois do primeiro uso).

O resultado alimenta o modo card: volte ao passo 3, normalmente pelo fatiamento (item h).

### 5. Artefato e bloco montados

Monte os dois textos com as respostas confirmadas. Mostre-os ao Fabiano e só grave depois da confirmação. Ordem de gravação:

1. Grave o artefato (Gravar o artefato).
2. Grave o corpo (Regras de edição do corpo).
3. Só então grave o Porte no campo "Porte" do Project e mova o Status para "Pronto p/ Refinamento", com `gh project item-edit` e os IDs descobertos no `gh project field-list`, sem fixar IDs.

Exceções ao passo 3, em que o Status fica em Concepção e o Porte não é gravado:

- **Elefante:** grave o artefato com o fatiamento e a sequência. Se o bloco do corpo é gravado no card original: decidir no primeiro elefante (pergunte ao Fabiano).
- **Rato que é tarefa de outra entrega:** grave o artefato e espere a decisão do Fabiano sobre o destino do card (a definir).

Depois de gravar, diga o que foi feito e o que ficou para o Fabiano (Situação, Issue type, criação de cards, se for o caso).

#### Artefato "Concepção"

Escreva com a ferramenta Write em `/tmp/pode-gastar-card/<N>-concepcao.md`. Linha em branco entre todos os campos. Campos sem conteúdo ficam com "não se aplica".

```
# Concepção

**Modo:** card | descoberta

## CSD

**Certezas:** ...

**Suposições:** ...

**Dúvidas:** cada uma com "crítica" ou "não crítica" e o destino (respondida na Concepção, respondida no Refinamento, fora de escopo, Blocked)

## Investigações feitas

**Investigações:** o que foi lido no código e o que se concluiu

## Solução

**Solução definida:** ...

**Contrato ou protótipo:** ...

## Riscos e mitigação

**Riscos:** cada risco com a mitigação (usabilidade, viabilidade técnica, outras áreas)

## INVEST por fatia

**Fatia 1:** Independente ... Negociável ... Valiosa ... Estimável ... Pequena ... Testável ...

## Fora de escopo

**Fora de escopo:** ...

## Porte

**Porte:** rato | cachorro | elefante

**Pergunta de valor (se rato):** Story | tarefa de outra entrega | Dívida

## Fatiamento e sequência (se elefante)

**Fatias:** ...

**Sequência e dependências:** ...

## Decisões do Fabiano

**Decisões:** ...
```

#### Bloco no corpo

Vai depois do "## Resumo do Entendimento", que já traz o problema e o que se ganha. NÃO repita o problema. Se uma fatia redefinir o problema, diga só isso, em uma linha.

```
## Concepção

**Solução definida:** ...

**Critérios de aceite:**
- CA1: ...

**Porte:** rato | cachorro | elefante

**Riscos que afetam o agente:**
- risco: mitigação, em uma linha

**Seções da spec autorizadas:** spec.md: ...; operacao.md: ...

**Cenários (desejável):** ...

**Detalhe:** artefato "Concepção" desta issue
```

### Regras de edição do corpo

O corpo é um campo único e só o bloco "## Concepção" é desta skill. Portanto:

1. Leia o corpo atual: `gh issue view <N> --json body --jq .body`. Com a ferramenta Write, grave a cópia exata em `/tmp/pode-gastar-card/<N>-antigo.md`.
2. Nunca altere o que não é da skill (o texto da criação e o "Resumo do Entendimento"). Acrescente o bloco depois do "Resumo do Entendimento", ou substitua o próprio bloco (do título "## Concepção" até o próximo "## " ou o fim do corpo) se a Concepção for refeita.
3. Escreva o corpo novo com a ferramenta Write em `/tmp/pode-gastar-card/<N>-novo.md`. Antes de gravar, compare: `diff /tmp/pode-gastar-card/<N>-antigo.md /tmp/pode-gastar-card/<N>-novo.md`. Tudo fora do bloco tem de estar idêntico. Se não estiver, pare e não grave.
4. Grave o corpo inteiro de volta: `gh issue edit <N> --body-file /tmp/pode-gastar-card/<N>-novo.md`.

### Gravar o artefato

1. Veja se o artefato já existe: `gh issue artifact list <N> --json number,name`. A saída é uma lista JSON (`[]` se não houver artefato). Procure `"name": "Concepção"`.
2. Se não existir, crie: `gh issue artifact create <N> /tmp/pode-gastar-card/<N>-concepcao.md#Concepção`. O `create` devolve o número do artefato na própria saída: use esse número nos passos seguintes.
3. Se já existir, substitua o conteúdo: `gh issue artifact edit <N> <número-do-artefato> --body-file /tmp/pode-gastar-card/<N>-concepcao.md`. O `edit` substitui o conteúdo inteiro: na retomada, o arquivo novo tem de trazer o que já estava decidido no artefato, mais o que foi decidido agora.
4. Confira: `gh issue artifact view <N> <número-do-artefato>`.

Pelo `--help` do gh 2.105.0, o `edit` guarda as versões: o `view` mostra o histórico de edições e o `edit --restore-version` recupera uma versão anterior. A verificar no piloto.
