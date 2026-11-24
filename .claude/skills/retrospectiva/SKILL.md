---
name: retrospectiva
description: Faz a retrospectiva de um card que chegou a Done. Use quando o Fabiano pedir a retrospectiva de um card, numa sessão nova: mede o card com o script, conversa com ele, cruza o que ele diz com os dados e fecha com um plano de ação gravado fora do repositório.
---

# Retrospectiva

Etapa depois do Done no GitHub Project "Pode Gastar". Quem chama é o Fabiano, numa sessão nova, depois que o card chega a Done. Nada dispara sozinho, e o agente não roda a retrospectiva por conta própria. O fluxo dos cards está na ADR 0015; a decisão desta skill, na ADR 0016.

A retrospectiva é por card: mede o card, conversa com o Fabiano, cruza o que ele diz com os dados e fecha com um plano de ação. A visão do fluxo todo fica para depois.

O resultado fica fora do repositório, na pasta cujo caminho está no `CLAUDE.md` (linha "Pasta das retrospectivas"):

- `retrospectivas.csv`: uma linha por retrospectiva, a base histórica. As colunas são as do script de medição.
- `card-<N>.md`: a parte de texto da retrospectiva do card `<N>`. O `id_card` liga a linha do CSV ao arquivo.

## Princípios

- Português simples. Passos curtos.
- UMA pergunta por vez. Espere a resposta antes da próxima.
- Você propõe e o Fabiano decide, nas palavras dele. Nunca preencha lacuna com palpite.
- Dado é dado: não invente nem estime número. Célula que o script deixou vazia continua vazia; o que o script marcou como "estimado" é dito como estimado.
- Toda ação do plano registra o chapéu (PM, Tech Lead ou QA).
- Você é facilitador de conclusão:
  - No início, mostre o roteiro: medição, conversa, conferência do plano anterior, quem levantou os achados, plano de ação, gravação.
  - A cada passo, diga o que já foi coberto e o que falta.
  - Se um tópico passar de três voltas, diga que a conversa está girando nele e proponha uma saída: decidir agora, estacionar ("Fica para depois") ou virar ação do plano.
- A skill só LÊ o GitHub. Você NUNCA comenta em card ou PR, NUNCA muda Status nem Situação, NUNCA cria card, NUNCA fecha issue. Nada do plano vira card: tudo fica no `card-<N>.md`.

## Regras de ferramentas

- Medição: só pelo script `.github/scripts/retrospectiva-medir.sh <N>`. Ele só faz consultas (query) e recusa mutation. Não chame `gh api graphql` direto: a exceção da ADR 0016 vale só para o script. O Claude Code vai pedir permissão para rodar o script e para acessar a pasta das retrospectivas; o Fabiano aprova na hora.
- Leitura no GitHub, se precisar de contexto além do script: `gh issue view`, `gh pr view`, `gh issue artifact list` e `gh issue artifact view`. Nada que escreva.
- Arquivos temporários: ferramenta Write em `/tmp/pode-gastar-card/` (se o arquivo existir, leia com Read antes). Não use `sed`, `tail`, `mktemp` nem `>`.
- Leitura e busca nos arquivos (CSV, `card-<N>.md`, `CLAUDE.md`): ferramentas Read e Grep, não `cat`, `tail` nem `grep` pelo Bash.
- Arquivos da pasta das retrospectivas:
  - `card-<N>.md`: ferramenta Write. Se o arquivo já existir, leia com Read antes.
  - `retrospectivas.csv`: só acréscimo no fim (Gravar, passo 7). Nunca reescreva o arquivo nem linhas antigas. É a única exceção à regra do `>`: a ferramenta Write reescreveria o arquivo inteiro, então a linha nova entra com `cat <arquivo-da-linha> >> <pasta>/retrospectivas.csv`, e só ali.

## Entrada

1. Receba o número do card. Se faltar, pergunte.
2. Leia o caminho da pasta no `CLAUDE.md`, na linha "Pasta das retrospectivas". Se a linha não existir, pare e avise.
3. Rode o script uma vez e guarde a saída (o passo 1 usa a mesma saída):

   ```
   .github/scripts/retrospectiva-medir.sh <N>
   ```

4. Confira o campo `status_atual`. Se não for "Done", avise e pare. O Fabiano pode mandar seguir mesmo assim; nesse caso, diga que o último Status conta até o momento da medição e que o total fica vazio.
5. Confira a pasta e o CSV. Se a pasta ou o `retrospectivas.csv` não existir, diga o que vai criar e peça confirmação. Com o sim:
   - `mkdir -p <pasta>`
   - Crie o CSV com a ferramenta Write, só com o cabeçalho, que é a saída de `.github/scripts/retrospectiva-medir.sh --cabecalho` (e o campo `csv_cabecalho` do JSON).
   Se o CSV existir, confira que o cabeçalho é igual ao do script. Se for diferente, pare e avise: não misture colunas.
6. Leia as últimas retrospectivas, para comparar e conferir o plano anterior:
   - As 3 últimas linhas do CSV: leia o arquivo com Read.
   - O plano de ação da retrospectiva mais recente: a última linha do CSV dá o `id_card`; leia a seção "Plano de ação" do `card-<id>.md`. Se não houver retrospectiva anterior, diga isso e siga.
7. Se o card já tem linha no CSV (Grep por `^<N>,` no `retrospectivas.csv`), mostre a linha e pergunte se ele quer refazer. Refazer não apaga nada: a nova linha é acrescentada no fim, vale a mais recente de cada `id_card` e o `card-<N>.md` é substituído (depois de lido).

## Passo a passo

### 1. Medição

Mostre o resultado em tabela curta:

- Tempo em cada Status (campo `tempos`, com o `legivel`), com fila ou ativo ao lado.
- Total, fila e ativo, voltas de Status, CI verde na primeira, evidência de QA, roteiro marcado, pendências e sugestões do revisor, edições do Refinamento, comentários de decisão e desvios.
- O que ficou vazio (`nao_encontrado`) e o que é estimado (`estimado`), em uma linha cada. Se houver `avisos` ou `fora_de_fila_e_ativo`, diga.

Depois, destaque de dois a quatro pontos que merecem investigação, escolhidos pelos dados, com o número que levou a cada um:

- a maior fila (o Status de fila com mais tempo);
- retrabalho: voltas de Status, muitas edições do Refinamento;
- pendências do revisor ou CI vermelho na primeira;
- QA sem evidência (`evidencia_qa` = "nao");
- roteiro não marcado (menos de 100%);
- desvios do roteiro (estimado);
- comparação com as últimas retrospectivas, quando o número mudou muito.

Não destaque mais de quatro. Diga quais ficaram de fora.

### 2. Pergunta aberta

Uma pergunta só: como foi o card, o que mais atrapalhou e o que funcionou. Deixe o Fabiano falar. Anote os achados.

### 3. Perguntas guiadas

Uma pergunta por vez, seguindo os pontos destacados que a pergunta aberta não cobriu. Em cada resposta:

- Quando o que ele diz se apoia nos dados, diga, com o número.
- Quando os dados contradizem, diga também, com o número, sem julgar. Ele decide qual leitura fica.
- Dado estimado ou vazio não confirma nem contradiz nada: diga isso.

Cada ponto termina com uma conclusão de uma linha, nas palavras dele.

### 4. Conferência do plano anterior

Se a retrospectiva anterior tem ações com resultado esperado, confira cada uma com os dados deste card, usando o "como medir" dela: veio, não veio ou não dá para medir (diga por quê). Ação que não deu resultado: pergunte se continua, muda ou sai. Sem retrospectiva anterior ou sem ação com resultado esperado: diga isso e siga.

### 5. Quem levantou e nota de atrito

- Liste os achados relevantes e pergunte quem levantou cada um: a skill (as perguntas ou os dados da retrospectiva), o Fabiano ou o agente (durante o trabalho do card). As contagens vão para `achados_skill`, `achados_fabiano` e `achados_agente`.
- Pergunte a nota de atrito do card, de 1 a 5 (1 = fluiu sem atrito; 5 = muito atrito). Vai para `nota_atrito`.

### 6. Plano de ação

Proponha poucas ações, ligadas aos achados. Cada ação tem:

- **Ação:** o que muda, em uma linha.
- **Chapéu:** PM, Tech Lead ou QA.
- **Motivo:** o achado e o dado que levaram a ela.
- **Resultado esperado:** o que deve mudar no próximo card.
- **Como medir:** a coluna do CSV ou o sinal que confirma, para a conferência da próxima retrospectiva.
- **Onde entra:** CLAUDE.md, skill (qual), ruleset, ADR, template ou outro.

O Fabiano decide o que entra, o que muda e o que vai para "Fica para depois". Nada vira card, e você não altera nenhum arquivo do repositório: a ação fica registrada no `card-<N>.md`, e quem a executa depois é o Fabiano.

### 7. Gravar

1. Monte a linha do CSV: o campo `csv_linha` do script termina com as quatro células da conversa vazias (`,,,,`). Troque-as, nesta ordem, por `achados_skill`, `achados_fabiano`, `achados_agente` e `nota_atrito`. Não mude nenhuma outra célula.
2. Monte o `card-<N>.md` pelo modelo abaixo.
3. Mostre os dois ao Fabiano e só grave depois da confirmação dele.
4. Grave o `card-<N>.md` com a ferramenta Write.
5. Acrescente a linha ao CSV, sem reescrever o arquivo:
   - Grave só a linha, com a ferramenta Write, em `/tmp/pode-gastar-card/<N>-retrospectiva-linha.csv`.
   - Leia o CSV com Read e anote o número de linhas.
   - Acrescente (a exceção das Regras de ferramentas): `cat /tmp/pode-gastar-card/<N>-retrospectiva-linha.csv >> <pasta>/retrospectivas.csv`.
   - Leia o CSV de novo com Read: tem de ter uma linha a mais, e a última é a linha nova. As anteriores, idênticas.
6. Diga o que foi gravado e onde. Lembre que os dados ficam fora do repositório e sem backup.

## Modelo do `card-<N>.md`

Campo sem conteúdo fica com "não se aplica".

```
# Retrospectiva do card #<N>: <título>

Data: <AAAA-MM-DD>. Done em <data_done>. PR #<número>.

## Resumo

<três linhas: como foi, o principal achado, a principal ação>

## Medição

| Medida | Valor |
| --- | --- |
| Tempo total (Entendimento até Done) | ... |
| Fila / ativo | ... / ... |
| <cada Status com tempo> | ... |
| Voltas de Status | ... |
| CI verde na primeira | ... |
| Evidência de QA | ... |
| Roteiro marcado | ... |
| Pendências / sugestões do revisor | ... / ... |
| Edições do Refinamento | ... |
| Comentários de decisão / desvios (estimado) | ... / ... |
| Nota de atrito | ... |

Não encontrado: ... Estimado: ...

## Pontos investigados e o que o Fabiano disse

- **<ponto>** (<dado>): <o que ele disse>. Conclusão: ...

## Cruzamento com os dados

- <afirmação>: apoiada | contradita | sem dado, com o número

## Conferência do plano anterior

- <ação da retrospectiva do card #X>: veio | não veio | não dá para medir, com o dado

## Plano de ação

| Ação | Chapéu | Motivo | Resultado esperado | Como medir | Onde entra |
| --- | --- | --- | --- | --- | --- |
| ... | ... | ... | ... | ... | ... |

## Fica para depois

- ...
```
