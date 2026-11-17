# Spec: Pode Gastar (v8)

Status: v8 aprovada e mesclada (PR #18 e #19). A revisão de operação de 17/11/2026 (`/health` com commit, deploy e rollback, ADR 0013) foi mesclada pela PR #23. As decisões das regras complementares da operação (O9 a O14) foram aprovadas pelo Fabiano em 17/11/2026; o texto delas e os critérios de aceite CA21 a CA52 aguardam a aprovação da PR #24 (`docs/rollback-spec-adr-0013`). Substitui a v7. Escopo de decisão fechado. Grupo, identificação de usuário, permissões e aprovação ficam para as funcionalidades futuras. Mudanças futuras da spec vão em PR próprio, separado da implementação, conforme o CLAUDE.md.

## Histórico

- **v8, critérios de aceite e regras complementares da operação (2026-11-17):** CA21 a CA52 novos, para O2 a O14. Decisões aprovadas pelo Fabiano em 17/11/2026, na conversa de design desta revisão (sem resumo de design separado; `docs/design/health-commit-rollback.md` não foi alterado): o valor de `RENDER_GIT_COMMIT` é aparado, e vazio ou só com espaços vale `"desconhecido"` (O2, O3); qualquer 2xx da API do Render aceita o pedido (O8); 3xx, sem seguir o redirecionamento, 4xx, 5xx e falha de rede fazem o rollback falhar na hora, sem nova tentativa, e 1xx fica fora da regra (O9, nova); na espera, código diferente de 200, conexão recusada, falha de DNS ou TLS, conexão interrompida, resposta sem `commit` legível e consulta acima do tempo limite de 5 segundos contam como "ainda não" (O10, nova); no rollback, o prazo começa quando a API aceita o pedido com 2xx; a fila do GitHub não conta no prazo (O4, O7); o intervalo conta do fim de uma consulta ao início da próxima (O13, nova); o rollback só roda na `main` e, fora dela, o job aparece como "pulado" (O11, nova); deploy hook e smoke test viram um único job no `ci.yml`, no mesmo grupo de concorrência do rollback, e quem chega depois espera (O12, nova); no deploy normal, o prazo começa na resposta 2xx do deploy hook, e hook fora de 2xx ou erro de rede faz o job falhar na hora (O4); `commit` nulo, numérico, vazio, corpo que não é JSON e `"desconhecido"` contam como "ainda não" e falham no fim do prazo (O10); os segredos de deploy ficam no environment `production`, restrito à `main` (O14, nova); CA37, CA38, CA42 e CA52 são verificados à mão numa execução real. A ADR 0013 registra os nomes dos segredos da API do Render. Os Abertos ganham o item do `imgURL` no deploy hook, os pontos que ficaram sem decisão e o risco aceito do cancelamento cruzado na fila; sai o item "Papel do design". Fonte: PR #24 (`docs/rollback-spec-adr-0013`).
- **v8, revisão de operação de 2026-11-17:** `/health` passa a expor o commit; regras do deploy e do rollback (seção "Operação"); CA13 atualizado. Design: `docs/design/health-commit-rollback.md`. Decisão: ADR 0013. Fonte: PR #23 (`docs/health-commit-rollback`).
- **v8, revisão de 2026-11-17:** registra as decisões da Etapa C (ADR 0010, 0011 e 0012) na R6 e no contrato, sem critério de aceite novo. Fontes: PR #18 (spec v8), PR #20 (código da Etapa C) e PR #21 (`docs/spec-v8-decisoes`). Pendências em "Abertos".
- **v8 (2026-11-16):** aprovada e mesclada na main pela PR #18. Decisões do Fabiano de 16/11/2026, sobre a v7.
  - R6: regra de corte nova. Corta primeiro o menos importante (`EM_DISCUSSAO`, `SERIA_LEGAL`, `QUERO_FAZER`, `DEVO_FAZER`); do mais importante, só o mínimo que faltar. Dentro do grupo, a combinação passa a ser ordenada por menor soma de valores, depois menor soma de intensidades, depois menor id (na v7, a intensidade vinha antes do valor).
  - R6: `PARCIAL` entra nos grupos de corte, junto com `PENDENTE`, pelo cortável (planejado − realizado). `PARCIAL` que já gastou além do previsto não entra.
  - R6: na compra `QUERO_FAZER`, o `DEVO_FAZER` `PENDENTE` ou `PARCIAL` entra como último grupo. Se nem assim cobre, a resposta traz a lista completa e quanto ainda faltaria.
  - R6: em `NAO_CABE` por falta de `SERIA_LEGAL`, `falta` é o que ainda faltaria depois de cortar todos os elegíveis. O contrato registra isso.
  - R5 e R7: `DEVO_FAZER` `PARCIAL` entra nas opções 1 a 3 da compra `DEVO_FAZER`, junto com `PENDENTE`.
  - R7: passo a passo da compra `DEVO_FAZER`: regra de corte da R6 e, se ela não cobre, as opções 1 a 3. As opções 1 e 3 escolhem o `DEVO_FAZER` por menor intensidade, depois menor cortável, depois menor id. Opções com os mesmos itens valem uma só.
  - R7: ordem única da lista completa. Ela só aparece quando a soma dos elegíveis não cobre a falta, então o que ainda faltaria é sempre maior que zero.
  - Contrato: valores do campo `opcao` (`PADRAO`, `OPCAO_1`, `OPCAO_2`, `OPCAO_3`).
  - Critérios de aceite: CA14 a CA20 e CB5 novos; CA10 reforçado; CA5 reescrito (o caso é resolvido pela regra de corte, não por uma opção).
  - Limite de 20 itens (6a) segue adiado e continua valendo por grupo.
  - A v7 foi aprovada e mesclada na main pela PR #10.
- **v7 (2026-11-12):**
  - R6: as combinações passam a ser ordenadas por menor soma de intensidades, depois por menor soma de valores, depois pelo menor id.
  - R11 (nova): em compra `SERIA_LEGAL`, só entram nas sugestões itens `SERIA_LEGAL` e `EM_DISCUSSAO`.
  - O item ganha `id`, identificador interno único dentro do mês. O nome serve só para exibição, e itens com o mesmo nome continuam sendo itens diferentes.
  - A resposta da avaliação ganha `avisos`, com uma mensagem por nome repetido no mês. A comparação de nomes ignora maiúsculas e espaços nas pontas.
  - Contrato: novo erro 422 para grupo com mais de 20 itens ao calcular as sugestões.
  - Os textos exibidos ao usuário vêm prontos do serviço de aplicação; a mensagem de `EM_DISCUSSAO` (R8) vira o campo `mensagem` do item.
  - Pendência registrada: o cartão de crédito ainda é reconhecido pelo nome.
- **v6:** aprovada e mesclada na main pela PR #1. Escopo de decisão fechado. Substituiu a v5.

## Problema

Durante o mês, quero saber se uma compra cabe no orçamento sem tirar dinheiro de itens que já planejei. Se não couber, quero saber por quê. Se for possível couber cortando itens, quero ver quais itens sairiam, com a menor perda.

## Conceitos

- **Renda:** entrada do mês.
- **Custo fixo:** total das contas recorrentes do mês.
- **Investimento:** valor decidido no início do mês, com status `PLANEJADO` (ainda pode mudar) ou `APLICADO` (saiu da conta e não volta atrás).
- **Orçamento variável:** renda, menos custo fixo, menos investimento.
- **Mês:** conjunto de renda, custo fixo, investimento e custos variáveis. Fica guardado no servidor.
- **Custo variável:** gasto que não é fixo nem investimento. Cada item tem:
  - id: identificador interno do item, único dentro do mês;
  - nome: usado só para exibição;
  - categoria: `DEVO_FAZER`, `QUERO_FAZER` ou `SERIA_LEGAL`;
  - intensidade: inteiro de 1 a 5;
  - planejado: sim ou não;
  - valor estimado: o valor previsto para o item, planejado ou não;
  - valor realizado: o que já foi gasto, quando houver gasto;
  - status: `PENDENTE`, `EM_DISCUSSAO`, `PARCIAL` ou `REALIZADO`;
  - decisor responsável: só para `EM_DISCUSSAO`.
  - A identidade do item é o `id`. Dois itens com o mesmo nome continuam sendo itens diferentes: nunca se fundem nem se escondem por causa do nome.
- **Compra:** simulação de um gasto novo, com valor e categoria. Não vira item do mês.

### Status dos itens

- `PENDENTE`: planejado, sem gasto. Reserva o valor estimado.
- `EM_DISCUSSAO`: item não planejado, solicitado durante o mês, ainda não decidido. Reserva o valor estimado.
- `PARCIAL`: já houve gasto, mas ainda vão acontecer outros. Reserva o que falta do planejado.
- `REALIZADO`: encerrado. Nenhum gasto a mais.

## Cálculo

- **Valor comprometido** de cada item:
  - `PENDENTE` ou `EM_DISCUSSAO`: valor estimado.
  - `PARCIAL`: o maior entre o valor estimado e o valor realizado.
  - `REALIZADO`: valor realizado.
- **Valor já saído** de cada item: valor realizado, para `PARCIAL` e `REALIZADO`. Zero para os demais.
- **Total restante** = orçamento variável − soma dos valores já saídos.
- **Livre** = orçamento variável − soma dos valores comprometidos.

## Regras de decisão

- **R1.** Valor da compra menor ou igual a zero é entrada inválida (400).
- **R2.** Se a compra cabe no livre, a decisão é `APROVADO`. Saldo restante = livre − compra.
- **R3.** Se a compra não cabe no livre, mas cabe no total restante, a decisão é `CABE_COM_CORTES`. Falta = compra − livre.
- **R4.** Se a compra é maior que o total restante, a decisão é `NAO_CABE`. Cortar itens não resolve, porque cortar não altera o total restante. Motivo: "compra excede o total que ainda sobra no mês". Falta = compra − total restante. A compra `NAO_CABE` por esta regra, como a `APROVADO` (R2), não passa pela checagem do limite de 20 itens (R6).
- **R5.** Elegíveis para corte:
  - `EM_DISCUSSAO`, de qualquer categoria, incluindo `DEVO_FAZER`.
  - `PENDENTE` de `SERIA_LEGAL` e `QUERO_FAZER`, conforme R6.
  - `PARCIAL`. Cortar cancela só o restante. O item passa a `REALIZADO` com o valor já gasto.
  - `PENDENTE` ou `PARCIAL` de `DEVO_FAZER`, só como último recurso: último grupo da compra `QUERO_FAZER` (R6) e opções da compra `DEVO_FAZER` (R7).
  - Nunca `REALIZADO`. Nunca o cartão de crédito.
- **R6.** Grupos de corte e regra de corte.
  - Os itens elegíveis (R5) formam grupos, do menos importante para o mais importante:
    - Grupo 0: itens `EM_DISCUSSAO`, de qualquer categoria.
    - Grupo 1: `SERIA_LEGAL` `PENDENTE` ou `PARCIAL`.
    - Grupo 2: `QUERO_FAZER` `PENDENTE` ou `PARCIAL`.
    - Grupo 3: `DEVO_FAZER` `PENDENTE` ou `PARCIAL`.
  - Grupos de cada compra:
    - Compra `SERIA_LEGAL`: só os grupos 0 e 1. Nenhum `QUERO_FAZER` ou `DEVO_FAZER` `PENDENTE` ou `PARCIAL` é sugerido como troca (R11). Se não bastarem, a decisão é `NAO_CABE`, com motivo "não há itens seria legal suficientes para cobrir a compra". Nesse caso, `falta` é o que ainda faltaria depois de cortar todos os itens dos grupos 0 e 1.
    - Compra `QUERO_FAZER`: grupos 0, 1, 2 e 3. O grupo 3 é o último recurso.
    - Compra `DEVO_FAZER`: grupos 0, 1 e 2. O grupo 3 só entra pelas opções da R7.
  - Cortável de cada item: o valor estimado, para `PENDENTE` e `EM_DISCUSSAO`, que saem inteiros; planejado − realizado, para `PARCIAL`, porque o corte cancela só o restante. Item `PARCIAL` que já gastou além do previsto (cortar não libera valor) não entra em nenhum grupo.
  - **Regra de corte:** corta primeiro o menos importante; do mais importante, só o mínimo que faltar.
    1. Se a soma dos cortáveis de todos os grupos da compra não cobre a falta, não há sugestão: a compra `SERIA_LEGAL` é `NAO_CABE`, a compra `QUERO_FAZER` recebe a lista completa (R7) e a compra `DEVO_FAZER` segue para as opções (R7).
    2. Se cobre, a conta vai do grupo mais importante da compra para o menos importante. Em cada grupo, calcula-se quanto da falta que resta os grupos menos importantes, inteiros, não conseguem cobrir. Se esse valor é zero ou menos, nenhum item do grupo entra. Se é maior que zero, entra a combinação de itens do grupo que cobre esse valor com a menor soma de cortáveis. O valor cortado no grupo é descontado da falta que resta, e a conta segue para o grupo anterior.
    3. Empates entre combinações do mesmo grupo, nesta ordem: menor soma das intensidades; menor id. Para comparar duas combinações, os ids de cada uma são ordenados e comparados um a um, até o primeiro diferente; se todos os ids comuns forem iguais, a combinação com menos itens vem antes. Combinações usam a ordem numérica natural dos ids (`item-2` antes de `item-10`, ADR 0011), inclusive na opção 2 da R7, e os demais desempates por id (opções 1 e 3 da R7, ordem dentro da sugestão e lista completa) usam a ordem de texto.
  - Uma combinação pode ter quantos itens forem necessários. Nenhum item entra na sugestão sem ser necessário.
  - **Limite de 20 itens (ADR 0012).** A busca testa todas as combinações do grupo, então nenhum grupo com mais de 20 itens chega a ela. Acima do limite, a resposta é 422.
    - Grupo 0 (em discussão): a contagem inclui todos os itens `EM_DISCUSSAO`, inclusive os de valor zero. A checagem vale em qualquer compra, antes de qualquer combinação, mesmo que o grupo não fosse usado e inclusive quando a compra iria direto para a lista completa. Na compra `SERIA_LEGAL`, mais de 20 itens em discussão dá 422 em vez de `NAO_CABE`.
    - Grupos 1 a 3: a contagem considera só itens com valor de corte maior que zero. Nos grupos 1 e 2, a checagem vale só quando o grupo é combinado. O grupo 3 da compra `QUERO_FAZER` é checado antes de qualquer combinação, como o grupo 0.
    - Opção 2 da R7: `QUERO_FAZER` e `DEVO_FAZER` contam juntos.
  - A sugestão traz os itens na ordem dos grupos. Dentro do grupo, menor intensidade e depois menor id (R6). No grupo 0, a ordem é pela categoria (seria legal, depois quero, depois devo) e só depois pela intensidade (menor primeiro).
- **R7.** Compra `DEVO_FAZER` e lista completa.
  - **Compra `DEVO_FAZER`, passo a passo.**
    1. Aplica-se a regra de corte da R6 aos grupos 0, 1 e 2. Se cobre a falta, essa é a única sugestão e a conta termina.
    2. Se não cobre, há três opções. Elas são alternativas, para a pessoa escolher entre cortar mais `QUERO_FAZER` ou sacrificar um `DEVO_FAZER`. Por definição, em todas entram todos os itens `EM_DISCUSSAO` e todos os `SERIA_LEGAL` elegíveis. `DEVO_FAZER` `PENDENTE` e `PARCIAL` entram juntos, pelo cortável.
       - **Opção 1:** acrescenta todos os `QUERO_FAZER` e um `DEVO_FAZER` cujo cortável, sozinho, cobre o que ainda falta depois deles. Entre os que cobrem, vence o de menor intensidade, depois menor cortável, depois menor id (R6).
       - **Opção 2:** acrescenta a combinação de itens `QUERO_FAZER` e `DEVO_FAZER` que cobre o que ainda falta depois dos `SERIA_LEGAL`. Vence a de menor soma das intensidades, depois menor soma dos cortáveis, depois menor id (R6).
       - **Opção 3:** acrescenta um `DEVO_FAZER` cujo cortável, sozinho, cobre o que ainda falta depois dos `SERIA_LEGAL`. Vence o de menor intensidade, depois menor cortável, depois menor id (R6).
       - Uma opção que não cobre a falta não existe. Opções com os mesmos itens (mesmos ids) valem uma só, e fica a primeira. As sugestões aparecem na ordem opção 1, 2, 3.
    3. Se nenhuma opção cobre, vale o item abaixo.
  - Se nenhuma opção cobrir a falta, é porque a soma de todos os itens elegíveis, incluindo `DEVO_FAZER` `PENDENTE` e `PARCIAL`, não cobre a falta (a opção 2 aceita qualquer número de itens). Nesse caso a decisão continua `CABE_COM_CORTES`, com a lista completa de itens elegíveis e o valor que ainda faltaria, sempre maior que zero. Motivo: "compra não cabe no livre; cabe no total restante com cortes; mesmo cortando todos os itens elegíveis ainda faltariam {valor que ainda faltaria}".
  - Compra `QUERO_FAZER` que nem a soma dos grupos 0 a 3 cobre: não usa as opções. A decisão é `CABE_COM_CORTES`, com `sugestoes` vazia, a lista completa e o valor que ainda faltaria, com o motivo do item acima.
  - Lista completa, para compra `QUERO_FAZER` ou `DEVO_FAZER`: primeiro os itens `EM_DISCUSSAO`, na ordem da R6; depois `SERIA_LEGAL`, `QUERO_FAZER` e `DEVO_FAZER` (`PENDENTE` ou `PARCIAL`), nessa ordem de categoria; dentro de cada categoria, menor intensidade primeiro; empate pelo id (R6). Só itens elegíveis pela R5.
- **R8.** Item `EM_DISCUSSAO` na sugestão traz o decisor responsável e o campo `mensagem`, com o texto "aguardando decisão de {decisor responsável}". O serviço de aplicação monta a mensagem (ver "Mensagens ao usuário").
- **R9.** Investimento: `APLICADO` é imutável. `PLANEJADO` pode ser editado, mas a edição fica fora desta fase.
- **R10.** A decisão só mostra o que sairia. Quem executa o corte fica fora desta fase.
- **R11.** Compra `SERIA_LEGAL`: só entram nas sugestões itens `SERIA_LEGAL` elegíveis (R5) e itens `EM_DISCUSSAO`, de qualquer categoria, como em R6. Itens `QUERO_FAZER` e `DEVO_FAZER` em qualquer outro status (`PENDENTE` ou `PARCIAL`) nunca entram nesse cálculo, nem quando os demais não cobrem a compra. Nesse caso a decisão é `NAO_CABE`, com o motivo da R6.

## Contrato da API (rascunho)

`GET /meses/{mesId}/avaliacao?valor={decimal}&categoria={categoria}`

- `mesId`: identifica o mês guardado no servidor. Como ele é obtido, por usuário ou por grupo, fica para a funcionalidade de grupo.
- `valor`: decimal positivo com até duas casas, por exemplo `150.00`.
- `categoria`: `DEVO_FAZER`, `QUERO_FAZER` ou `SERIA_LEGAL`.
- Não há nome na consulta. O nome só serve para exibição e vai com o registro do item, em uma funcionalidade futura.
- Como a consulta não altera nada, o método é GET.

Respostas:

- **200, `APROVADO`:** `livre` antes da compra, `saldoRestante` depois, sem sugestões.
- **200, `CABE_COM_CORTES`:** `totalRestante`, `livre`, `falta`, `motivo`, `sugestoes` (com itens e o valor que cobrem) e `listaCompleta` (quando não há combinação que cubra).
- **200, `NAO_CABE`:** `totalRestante`, `falta` e `motivo`, sem sugestões. `falta` é compra − total restante (R4) ou, na compra `SERIA_LEGAL` sem itens suficientes (R6), o que ainda faltaria depois de cortar todos os elegíveis.
- **400:** valor inválido ou categoria fora do conjunto. A mensagem aponta o campo.
- **404:** `mesId` não existe.
- **422:** grupo com mais de 20 itens ao calcular as sugestões (R6, "Limite de 20 itens"). A resposta segue o formato dos demais erros (`campo`, quando se aplica, e `mensagem`), por exemplo `"itens demais para calcular sugestões de corte neste mês (máximo 20 por grupo)"`.

Cada item em `sugestoes` e em `listaCompleta` traz `id`, `nome`, `status` e, só para `EM_DISCUSSAO`, `decisorResponsavel` e `mensagem`.

Cada sugestão traz `opcao`: `PADRAO`, quando vem da regra de corte (R6); `OPCAO_1`, `OPCAO_2` ou `OPCAO_3`, quando é uma das opções da compra `DEVO_FAZER` (R7).

Todas as respostas 200 trazem `avisos`, uma lista de textos, vazia quando não há nada a avisar. Quando há dois ou mais itens com o mesmo nome no mês, `avisos` traz uma mensagem por nome repetido, citando os nomes e os ids envolvidos. A comparação de nomes ignora maiúsculas e espaços no início e no fim: "Jantar fora" e " jantar FORA " são nomes repetidos. Nenhum item é removido nem escondido por causa disso.

### Mensagens ao usuário

Os textos exibidos ao usuário (o `motivo` da decisão, os `avisos` e a `mensagem` de item `EM_DISCUSSAO`) são montados pelo serviço de aplicação e vêm prontos na resposta. A tela só exibe.

Os erros de formato da requisição (valor inválido, campo obrigatório, categoria fora do conjunto) continuam sendo respondidos pelo adaptador web, com o 400 e o campo apontado.

Exemplo de resposta para `CABE_COM_CORTES` (os ids do exemplo são ilustrativos):

```json
{
  "decisao": "CABE_COM_CORTES",
  "totalRestante": "1600.00",
  "livre": "10.00",
  "falta": "140.00",
  "motivo": "compra não cabe no livre; cabe no total restante com cortes",
  "sugestoes": [
    {
      "opcao": "PADRAO",
      "itens": [
        {
          "id": "item-7",
          "nome": "Bolsa",
          "status": "EM_DISCUSSAO",
          "decisorResponsavel": "marido",
          "mensagem": "aguardando decisão de marido"
        },
        { "id": "item-6", "nome": "Bola de futebol", "status": "PENDENTE" }
      ],
      "cobre": "170.00"
    }
  ],
  "listaCompleta": [],
  "avisos": []
}
```

Exemplo de `APROVADO` em um mês com dois itens de mesmo nome:

```json
{
  "decisao": "APROVADO",
  "livre": "100.00",
  "saldoRestante": "20.00",
  "avisos": [
    "Nome repetido neste mês: \"Jantar fora\" (ids: item-5, item-9). São itens diferentes."
  ]
}
```

`GET /health`: ver "Operação" (O1 a O3).

## Operação

### `/health`

- **O1.** A resposta é `{"status": "UP", "commit": "<sha>"}`.
- **O2.** O valor de `commit` vem da variável de ambiente `RENDER_GIT_COMMIT`, que o Render disponibiliza no build e em tempo de execução. O valor é aparado: espaços em branco no início e no fim (espaço, tabulação, quebra de linha) são removidos antes de ser exposto e comparado.
- **O3.** Se a variável não existir, por exemplo ao rodar localmente, ou ficar vazia depois de aparada (vazia ou só com espaços), `commit` vale `"desconhecido"`. A falta da variável não faz o endpoint falhar: a resposta continua 200 com `"status": "UP"`.

### Deploy normal

- **O4.** Se o deploy hook responder fora de 2xx, ou se a chamada falhar por erro de rede, o job falha na hora, sem consultar `/health`. Com uma resposta 2xx do deploy hook, o smoke test consulta `/health` até o `commit` exposto ser igual ao SHA do commit publicado. Usa o mesmo intervalo e o mesmo prazo do rollback (O7, O13). O prazo de 10 minutos começa na resposta 2xx do deploy hook; o tempo de espera na fila do GitHub não conta. Se o prazo acabar antes disso, o smoke test falha.

### Rollback

- **O5.** O rollback recebe o SHA completo, com 40 caracteres, do commit que deve voltar ao ar. Ele chama `POST https://api.render.com/v1/services/{serviceId}/deploys` com `commitId` igual a esse SHA, autenticado pela chave da API do Render.
- **O6.** Se o SHA recebido não tiver exatamente 40 caracteres hexadecimais minúsculos, o rollback é rejeitado antes de qualquer chamada à API do Render, e o workflow falha com uma mensagem clara.
- **O7.** Depois que a API aceita o pedido da O5 com uma resposta 2xx (O8), consulta `/health` a cada 15 segundos, por até 10 minutos, até o `commit` exposto ser igual ao SHA pedido. O prazo de 10 minutos começa no momento em que a API aceita o pedido; o tempo de espera na fila do GitHub não conta. Se não chegar a esse SHA dentro do prazo, o rollback falha.
- **O8.** Qualquer resposta 2xx da API, por exemplo 201 ou 202, só significa que o pedido de deploy foi aceito. Ela não prova que o deploy terminou; só a O7 confirma o rollback.
- **O9.** Se a API do Render responder 3xx, 4xx ou 5xx, o rollback falha na hora, sem consultar `/health`, com uma mensagem que traz o código HTTP da resposta. Um 3xx não é seguido: o redirecionamento não é feito, e a resposta 3xx é a que conta. Respostas 1xx não entram nesta regra, porque o cliente HTTP as trata sozinho. Se a chamada não tiver resposta por falha de rede, o rollback também falha na hora, com uma mensagem clara, sem nova tentativa e sem consultar `/health`.

### Espera pelo commit

- **O10.** Na espera do smoke test (O4) e do rollback (O7), uma consulta ao `/health` com erro conta como "ainda não". Erro é: código diferente de 200, conexão recusada, falha de DNS, falha de TLS, conexão interrompida, resposta sem um campo `commit` legível (campo ausente, nulo, numérico ou vazio, ou corpo que não é JSON), `commit` igual a `"desconhecido"` ou consulta que passa do tempo limite de 5 segundos. Esse tempo limite vale para cada consulta e inclui a conexão e a leitura da resposta. As consultas continuam até o prazo. Se o prazo acabar em qualquer um desses casos, a espera falha.

### Execução dos workflows

- **O11.** O workflow de rollback só roda na `main`. O job tem uma condição que exige a `main`; disparado a partir de outra branch, o job não roda e aparece no GitHub como "pulado" (skipped), e nenhuma chamada é feita à API do Render nem ao `/health`. Esse é o comportamento esperado; a execução não aparece como falha.
- **O12.** No `ci.yml`, o deploy hook e o smoke test formam um único job, em sequência: primeiro o deploy hook, depois o smoke test. O grupo de concorrência fica nesse job e tem o mesmo nome do grupo do job de rollback. Assim, o deploy normal e o rollback nunca rodam ao mesmo tempo, e nenhum rollback roda entre o deploy hook e o smoke test. Quem chega depois espera, sem cancelar o que já está rodando (`cancel-in-progress: false`). Se houver mais de uma execução esperando no grupo, o GitHub cancela a que estava na fila e mantém só a mais nova.

### Intervalo e prazo das consultas

- **O13.** O intervalo de 15 segundos conta do fim de uma consulta ao início da próxima. Nenhuma consulta nova começa depois do fim do prazo. Uma consulta iniciada antes do fim do prazo vale, mesmo que termine depois dele. A regra do prazo vale para o rollback e para o deploy normal: o prazo começa na resposta 2xx da API do Render (O7) ou do deploy hook (O4). Nos testes automatizados, o script de espera recebe o intervalo e o prazo por variável de ambiente, usada só nos testes; os workflows usam os valores desta seção.

### Segredos de deploy

- **O14.** Os segredos de deploy (`RENDER_API_KEY`, `RENDER_SERVICE_ID` e `RENDER_DEPLOY_HOOK_URL`) ficam num environment do GitHub chamado `production`, restrito à branch `main`. Os jobs de deploy normal e de rollback usam esse environment. Um job que use o environment a partir de outra branch não recebe os segredos e não chega a rodar os passos.

## Mês de exemplo (estado no servidor)

Renda 6000. Custo fixo 3400. Investimento 1000, `APLICADO`. Orçamento variável = 1600.

| Item | Categoria | Intensidade | Planejado | Status | Valor estimado | Valor realizado |
|---|---|---|---|---|---|---|
| Cartão de crédito | DEVO_FAZER | 5 | sim | PENDENTE | 600 | — |
| Manutenção do carro | DEVO_FAZER | 5 | sim | PENDENTE | 200 | — |
| Viagem de fim de semana | QUERO_FAZER | 4 | sim | PENDENTE | 300 | — |
| Item para casa | QUERO_FAZER | 3 | sim | PENDENTE | 200 | — |
| Jantar fora no mês | SERIA_LEGAL | 2 | sim | PENDENTE | 120 | — |
| Bola de futebol | SERIA_LEGAL | 2 | sim | PENDENTE | 80 | — |

Sem a bolsa: total restante 1600, livre 100.

Com a bolsa em discussão (item adicional):

| Item | Categoria | Intensidade | Planejado | Status | Valor estimado | Decisor responsável |
|---|---|---|---|---|---|---|
| Bolsa | QUERO_FAZER | 3 | não | EM_DISCUSSAO | 90 | marido |

Com a bolsa: total restante 1600, livre 10 (1600 − 1500 − 90).

## Critérios de aceite

### Mês de exemplo base (sem a bolsa)

- **CA1.** `valor=80.00&categoria=QUERO_FAZER`: `APROVADO`, saldo restante 20, sem sugestão.
- **CA2.** `valor=300.00&categoria=QUERO_FAZER`: `CABE_COM_CORTES`, falta 200, sugestão jantar fora e bola de futebol, que somam 200.
- **CA3.** `valor=150.00&categoria=SERIA_LEGAL`: `CABE_COM_CORTES`, falta 50, sugestão bola de futebol.
- **CA4.** `valor=250.00&categoria=DEVO_FAZER`: `CABE_COM_CORTES`, falta 150, sugestão jantar fora e bola de futebol.
- **CA5.** `valor=800.00&categoria=DEVO_FAZER`: falta 700. A regra de corte fecha exato cortando os `SERIA_LEGAL` e os `QUERO_FAZER`, sem `DEVO_FAZER`; não chega às opções.
- **CA6.** `valor=1700.00&categoria=QUERO_FAZER`: `NAO_CABE`, motivo "compra excede o total que ainda sobra no mês", falta 100.
- **CA7.** Viagem `PARCIAL` com realizado de 150 (estimado 300): total restante 1450, livre 100. `valor=100.00&categoria=QUERO_FAZER`: `APROVADO`, saldo restante 0.
- **CA8.** Viagem `REALIZADO` com realizado de 250: total restante 1350, livre 150. `valor=150.00&categoria=QUERO_FAZER`: `APROVADO`, saldo restante 0.
- **CA9.** Caso CA8 com `valor=200.00&categoria=QUERO_FAZER`: falta 50, sugestão bola de futebol. A viagem, realizada, nunca aparece.
- **CA10.** Viagem `PARCIAL` com realizado de 150 e `valor=200.00&categoria=QUERO_FAZER` com livre insuficiente: a viagem é candidata a corte, cancelando só o restante de 150. Se for cortada, o item vira `REALIZADO` com valor 150. Reforço (v8): com a mesma viagem e `valor=600.00&categoria=QUERO_FAZER`, falta 500. Os `SERIA_LEGAL` (200) não bastam; a sugestão traz jantar fora, bola de futebol, item para casa e a viagem, que cobre 550. A viagem vira `REALIZADO` com 150.
- **CA11.** O cartão de crédito nunca aparece como sugestão de corte.
- **CA12.** `valor=0` ou negativo: 400.
- **CA13.** `GET /health` com `RENDER_GIT_COMMIT` definida: 200 com `{"status": "UP", "commit": "<valor da variável, aparado>"}`. Sem a variável: 200 com `{"status": "UP", "commit": "desconhecido"}` (O1 a O3).
- **CA14.** `valor=900.00&categoria=QUERO_FAZER`: `CABE_COM_CORTES`, falta 800, uma sugestão com jantar fora, item para casa, viagem e manutenção do carro, que somam 820. Os grupos menos importantes somam 700, então a manutenção do carro (200) é necessária. Depois dela e dos dois `QUERO_FAZER` (500), faltam 100, e o jantar fora (120) cobre sozinho. A bola de futebol fica de fora.
- **CA15.** `valor=1100.00&categoria=QUERO_FAZER`: `CABE_COM_CORTES`, falta 1000, `sugestoes` vazia, `listaCompleta` com jantar fora, bola de futebol, item para casa, viagem e manutenção do carro, nessa ordem, e ainda faltariam 100.
- **CA16.** `valor=500.00&categoria=SERIA_LEGAL`: `NAO_CABE`, motivo "não há itens seria legal suficientes para cobrir a compra", falta 200.
- **CA19.** `valor=600.00&categoria=QUERO_FAZER`: `CABE_COM_CORTES`, falta 500, uma sugestão com jantar fora (120), bola de futebol (80) e viagem (300), que somam 500. Os `SERIA_LEGAL` somam 200, então do `QUERO_FAZER` entra só o mínimo que cobre os 300 restantes: a viagem. O item para casa não entra, mesmo tendo menor intensidade que a viagem.
- **CA20.** `valor=400.00&categoria=QUERO_FAZER`: `CABE_COM_CORTES`, falta 300, uma sugestão com jantar fora (120) e item para casa (200), que somam 320. Os `SERIA_LEGAL` somam 200, então do `QUERO_FAZER` entra só o mínimo que cobre os 100 restantes: o item para casa. Depois dele faltam 100, e o jantar fora cobre sozinho. A viagem (300) cobriria a falta sozinha, mas é `QUERO_FAZER` e só entra depois dos `SERIA_LEGAL`.

### Outros meses (compra `DEVO_FAZER` com item `PARCIAL`)

- **CA17.** Mês com orçamento variável 700 e só três itens: jantar fora (`SERIA_LEGAL`, `PENDENTE`, 200), bola de futebol (`QUERO_FAZER`, `PENDENTE`, 100) e dentista (`DEVO_FAZER`, `PARCIAL`, planejado 300, realizado 100, cortável 200). Livre 100, total restante 600. `valor=600.00&categoria=DEVO_FAZER`: `CABE_COM_CORTES`, falta 500, uma sugestão com jantar fora, bola de futebol e dentista, que somam 500. `listaCompleta` vazia.
- **CA18.** Mês com orçamento variável 800 e só três itens: jantar fora (`SERIA_LEGAL`, `PENDENTE`, 200, intensidade 2), manutenção do carro (`DEVO_FAZER`, `PENDENTE`, 200, intensidade 5) e dentista (`DEVO_FAZER`, `PARCIAL`, planejado 300, realizado 100, cortável 200, intensidade 2). Livre 100, total restante 700. `valor=400.00&categoria=DEVO_FAZER`: `CABE_COM_CORTES`, falta 300, uma sugestão com jantar fora e dentista, que somam 400. O dentista entra no lugar da manutenção pela menor intensidade (2 contra 5), já que os dois cortam 200.

### Com a bolsa em discussão

- **CB1.** `valor=150.00&categoria=SERIA_LEGAL`: `CABE_COM_CORTES`, falta 140. Sugestão: bolsa (90), com "aguardando decisão de marido", mais bola de futebol (80). Somam 170 e sobram 30.
- **CB2.** `valor=80.00&categoria=QUERO_FAZER`: `CABE_COM_CORTES`, falta 70. Sugestão: bolsa, que sozinha cobre e sobra 20.
- **CB3.** A bolsa reclassificada como `DEVO_FAZER`, intensidade 2, ainda `EM_DISCUSSAO`: continua elegível e aparece primeiro na sugestão de `valor=150.00&categoria=SERIA_LEGAL`.
- **CB4.** Dois itens `EM_DISCUSSAO`, um `QUERO_FAZER` de intensidade 5 e um `DEVO_FAZER` de intensidade 2: o `QUERO_FAZER` vem antes na sugestão, porque a categoria pesa antes da intensidade.
- **CB5.** `valor=130.00&categoria=SERIA_LEGAL`: `CABE_COM_CORTES`, falta 120. Sugestão: bolsa (90) mais bola de futebol (80). Somam 170. O jantar fora (120) cobriria a falta sozinho, mas a bolsa, em discussão, sai primeiro; depois dela faltam 30, e a bola é o menor corte que cobre.

### Operação

- **CA21.** `GET /health` com `RENDER_GIT_COMMIT` definida, mas vazia: 200 com `{"status": "UP", "commit": "desconhecido"}`, igual à variável ausente (O3).
- **CA22.** Deploy normal do commit `<sha>`: depois do deploy hook, `/health` expõe outro commit nas primeiras consultas e `<sha>` numa consulta seguinte, dentro de 10 minutos. O smoke test passa nessa consulta e não consulta mais (O4).
- **CA23.** Deploy normal do commit `<sha>`: por 10 minutos, `/health` expõe outro commit ou `"desconhecido"`. O smoke test falha por prazo, com uma mensagem que traz o SHA esperado (O4).
- **CA24.** Rollback com um SHA de 40 caracteres hexadecimais minúsculos: o workflow faz um `POST` em `https://api.render.com/v1/services/{serviceId}/deploys` com o corpo `{"commitId": "<sha>"}`, autenticado pela chave da API do Render (O5).
- **CA25.** Rollback com um SHA de 39 caracteres, de 41 caracteres, com letra maiúscula ou com caractere fora de `0-9` e `a-f`: o workflow falha com uma mensagem clara sobre o formato do SHA, sem nenhuma chamada à API do Render nem ao `/health` (O6).
- **CA26.** Rollback para `<sha>`: a API responde 201 e `/health` expõe o commit anterior nas primeiras consultas e `<sha>` numa consulta seguinte, dentro de 10 minutos. O rollback termina com sucesso nessa consulta e não consulta mais (O7, O8).
- **CA27.** Rollback para `<sha>`: a API responde 202 e, por 10 minutos, `/health` não expõe `<sha>`. O rollback falha por prazo, com uma mensagem que traz o SHA pedido, mesmo com a resposta de sucesso da API (O7, O8).
- **CA28.** Rollback para `<sha>`: a API responde 401. O rollback falha na hora, com uma mensagem que traz o código 401, sem consultar `/health` (O9).
- **CA29.** Rollback para `<sha>`: a API responde 503. O rollback falha na hora, com uma mensagem que traz o código 503, sem consultar `/health` (O9).
- **CA30.** `GET /health` com `RENDER_GIT_COMMIT` igual a `"  <sha>  "`, com espaços antes e depois: 200 com `{"status": "UP", "commit": "<sha>"}`, sem os espaços (O2).
- **CA31.** `GET /health` com `RENDER_GIT_COMMIT` só com espaços: 200 com `{"status": "UP", "commit": "desconhecido"}` (O3).
- **CA32.** Rollback para `<sha>`: a API responde 200 e `/health` passa a expor `<sha>` dentro de 10 minutos. O rollback termina com sucesso (O8).
- **CA33.** Rollback para `<sha>`: a API responde 302 com um endereço em `Location`. O rollback falha na hora, com uma mensagem que traz o código 302, sem nenhuma chamada ao endereço do `Location` e sem consultar `/health` (O9).
- **CA34.** Rollback para `<sha>`: a chamada à API não tem resposta, por exemplo porque a conexão é recusada. O rollback falha na hora, com uma mensagem clara, depois de uma única tentativa e sem consultar `/health` (O9).
- **CA35.** Smoke test do deploy normal do commit `<sha>`: `/health` responde 502 numa consulta e expõe `<sha>` na consulta seguinte, dentro de 10 minutos. O smoke test continua depois do 502 e passa (O10).
- **CA36.** Rollback para `<sha>`, com a API respondendo 201: uma consulta ao `/health` não termina em 5 segundos, e a consulta seguinte expõe `<sha>`, dentro de 10 minutos. O rollback continua e termina com sucesso (O10).
- **CA37.** Workflow de rollback disparado a partir de uma branch diferente da `main`: o job não roda e aparece como "pulado", e não há chamada à API do Render nem ao `/health` (O11). Verificação manual numa execução real; passo a passo em "Verificação manual".
- **CA38.** Rollback disparado enquanto o job de deploy hook e smoke test de um deploy normal está rodando: o rollback espera o smoke test terminar e só então começa. O smoke test não é cancelado (O12). Verificação manual numa execução real; coberta pelos passos 1 a 4 do CA42 em "Verificação manual".
- **CA39.** Rollback para `<sha>`, com a API respondendo 201: por 10 minutos, toda consulta ao `/health` responde 502. O rollback falha por prazo, com uma mensagem que traz o SHA pedido (O7, O10).
- **CA40.** Smoke test do deploy normal do commit `<sha>`: a primeira consulta ao `/health` tem a conexão recusada, e a consulta seguinte expõe `<sha>`, dentro de 10 minutos. O smoke test continua depois da conexão recusada e passa (O10).
- **CA41.** Rollback para `<sha>`, com a API respondendo 201: a primeira consulta ao `/health` responde 200 sem um campo `commit` legível, por exemplo `{"status": "UP"}`, e a consulta seguinte expõe `<sha>`, dentro de 10 minutos. O rollback continua e termina com sucesso (O10).
- **CA42.** Com o job de deploy hook e smoke test rodando, um rollback está esperando no grupo de concorrência e um segundo rollback é disparado: o GitHub cancela o primeiro rollback, que estava na fila; o segundo passa a esperar; o job que estava rodando não é cancelado (O12). Verificação manual numa execução real; passo a passo em "Verificação manual".
- **CA43.** Rollback para `<sha>`, com a API respondendo 201: uma consulta ao `/health` começa, leva 3 segundos e expõe outro commit. A consulta seguinte começa 15 segundos depois do fim da anterior, ou seja, 18 segundos depois do início dela (O13).
- **CA44.** Rollback para `<sha>`: o workflow espera 3 minutos na fila do GitHub, e a API responde 201. `/health` expõe `<sha>` 9 minutos depois da resposta 201, ou seja, 12 minutos depois do disparo. O rollback termina com sucesso, porque o prazo começa na resposta 2xx (O7, O13).
- **CA45.** Deploy normal: o deploy hook responde 500. O job falha na hora, sem consultar `/health` (O4).
- **CA46.** Deploy normal: a chamada ao deploy hook não tem resposta, por exemplo porque a conexão é recusada. O job falha na hora, sem consultar `/health` (O4).
- **CA47.** Rollback para `<sha>`, com a API respondendo 201: por 10 minutos, toda consulta ao `/health` responde 200 com `{"status": "UP", "commit": null}`. Cada consulta conta como "ainda não", e o rollback falha por prazo, com uma mensagem que traz o SHA pedido (O10).
- **CA48.** Rollback para `<sha>`, com a API respondendo 201: por 10 minutos, toda consulta ao `/health` responde 200 com `{"status": "UP", "commit": 123}`. Cada consulta conta como "ainda não", e o rollback falha por prazo, com uma mensagem que traz o SHA pedido (O10).
- **CA49.** Rollback para `<sha>`, com a API respondendo 201: por 10 minutos, toda consulta ao `/health` responde 200 com `{"status": "UP", "commit": ""}`. Cada consulta conta como "ainda não", e o rollback falha por prazo, com uma mensagem que traz o SHA pedido (O10).
- **CA50.** Rollback para `<sha>`, com a API respondendo 201: por 10 minutos, toda consulta ao `/health` responde 200 com um corpo que não é JSON, por exemplo `UP`. Cada consulta conta como "ainda não", e o rollback falha por prazo, com uma mensagem que traz o SHA pedido (O10).
- **CA51.** Rollback para `<sha>`, com a API respondendo 201: por 10 minutos, toda consulta ao `/health` responde 200 com `{"status": "UP", "commit": "desconhecido"}`. Cada consulta conta como "ainda não", e o rollback falha por prazo, com uma mensagem que traz o SHA pedido (O10).
- **CA52.** Workflow de rollback disparado a partir de uma branch diferente da `main`, com o `rollback.yml` dessa branch alterado para remover a condição de `main`: o job não recebe os segredos do environment `production` e não chega a rodar os passos; não há chamada à API do Render nem ao `/health` (O14). Verificação manual numa execução real; passo a passo em "Verificação manual".

### Verificação manual

Estes passos usam uma execução real no GitHub e no Render, depois do merge da PR que implementa a seção "Operação". O CA37 e o CA52 usam o SHA que já está no ar e não trocam a versão publicada. O CA38 é coberto pelos passos do CA42. O CA42 aproveita o merge de uma PR já aprovada, que publica uma versão nova pelo deploy normal.

**CA37 (rollback fora da `main`):**

1. Abrir o `/health` de produção e anotar o `commit` exposto (`<sha-no-ar>`).
2. Em Actions, abrir o workflow de rollback, escolher "Run workflow", selecionar uma branch criada a partir da `main` depois do merge da implementação, sem alterar o workflow de rollback, e informar `<sha-no-ar>`.
3. Conferir que o job de rollback aparece como "pulado" e que a execução não aparece como falha.
4. Conferir no painel do Render que nenhum deploy novo foi criado, e que o `/health` continua expondo `<sha-no-ar>`.

**CA42 (cancelamento na fila):**

1. Mesclar na `main` uma PR já aprovada e anotar o SHA do merge (`<sha-do-merge>`). Esperar o job de deploy hook e smoke test dessa execução começar a rodar (execução A).
2. Com A rodando, disparar o workflow de rollback na `main` com `<sha-do-merge>` (execução B). Conferir que B aparece esperando.
3. Ainda com A rodando, disparar de novo o workflow de rollback na `main` com `<sha-do-merge>` (execução C). Conferir que B aparece cancelada pelo GitHub, que C aparece esperando e que A continua rodando.
4. Quando A terminar, conferir que C roda e termina com sucesso, e que o `/health` expõe `<sha-do-merge>`. C pede ao Render um novo deploy do mesmo commit, então a versão no ar não muda.
5. Se A falhar no smoke test, cancelar C manualmente antes de ele começar, porque `<sha-do-merge>` é o commit com falha, e seguir o procedimento normal de rollback para o commit anterior.

**CA52 (environment `production` restrito à `main`):**

1. Abrir o `/health` de produção e anotar o `commit` exposto (`<sha-no-ar>`).
2. Criar uma branch a partir da `main` e, nela, remover do `rollback.yml` a condição que exige a `main`. Não abrir PR.
3. Em Actions, disparar o workflow de rollback nessa branch, com `<sha-no-ar>`.
4. Conferir que o job não roda os passos.
5. Conferir no painel do Render que nenhum deploy novo foi criado, e que o `/health` continua expondo `<sha-no-ar>`. Apagar a branch de teste.

## Fora de escopo desta versão

- Grupo, identificação de usuário, papéis e permissões.
- Fluxo de solicitação, avaliação, aprovação e reprovação de itens.
- Quem pode executar um corte.
- Múltiplos decisores com aprovação de todos.
- Cadastro de item, inclusive não planejado, durante o mês.
- Telas de registro de gasto e de atualização de status.
- Atualização de custo fixo no meio do mês.
- Ajuste de investimento `PLANEJADO`.
- Persistência e banco de dados.
- Autenticação.
- Aplicativo mobile.

## Abertos

Para o escopo de decisão não há outra pergunta em aberto. Como o `mesId` é obtido, por usuário ou por grupo, fica para a funcionalidade de grupo.

- **Valor de corte zero (ADR 0010).** Inconsistência conhecida, pendente de decisão. `PENDENTE` com valor estimado zero e `PARCIAL` com realizado igual ao planejado ficam fora dos grupos e da lista completa; `EM_DISCUSSAO` com valor estimado zero entra no grupo 0 e na lista completa. A regra não muda nesta versão. Quando for decidida, a mudança vem em PR própria, com teste para os três casos.
- **CA10 e `REALIZADO`.** O CA10 diz que a viagem cortada vira `REALIZADO` com o valor já gasto, mas o contrato da resposta não tem esse campo (o item traz só `id`, `nome`, `status` atual e, para `EM_DISCUSSAO`, decisor e mensagem). Falta decidir se o contrato ganha o campo ou se o CA10 deixa de citar `REALIZADO`.
- **Cartão de crédito reconhecido pelo nome (R5, CA11).** Qualquer item com o nome "Cartão de crédito" é tratado como o cartão. A troca para um marcador próprio do item fica para depois.
- **Comparação de ids.** A comparação de ids não cobre maiúsculas e minúsculas, ids com prefixo comum nem trechos misturados de número e texto. Esses casos seguem o comportamento atual do código.
- **Critério de aceite que pode faltar.** Nenhum CA cobre o 422 do limite de 20 nem o desempate por ordem natural do id. Decidir se deve existir.
- **Publicação no GHCR.** O serviço no Render é construído a partir do GitHub (ADR 0013), então a imagem publicada no GHCR pode não ser o que roda em produção. Confirmar em outra PR se essa publicação ainda é necessária.
- **README descreve o fluxo por imagem.** O README descreve o smoke test e o rollback por imagem, sem conferir o commit. A PR que implementa a seção "Operação" não altera o README, por decisão do Fabiano de 17/11/2026. A atualização, para o README apontar para a spec em vez de descrever a regra, fica para PR própria.
- **Deploy hook envia `imgURL` do GHCR.** O passo do deploy hook no `ci.yml` ainda envia `imgURL` com a imagem do GHCR, mas o serviço no Render é construído a partir do GitHub (ADR 0013). Revisar em outra PR, junto com o item "Publicação no GHCR".
- **Cancelamento cruzado na fila (O12), risco aceito.** Pelo grupo de concorrência, um deploy normal que chega depois cancela um rollback que estava na fila, e um rollback que chega depois cancela um deploy normal que estava na fila. Risco aceito pelo Fabiano em 17/11/2026. Mitigação: não mesclar na `main` enquanto um rollback estiver em andamento.
- **Deploy hook com 3xx ou 1xx (O4).** A O4 trata "fora de 2xx" como falha, mas não diz se um 3xx do hook é seguido nem como fica um 1xx, como a O9 diz para a API do Render.
- **Nova tentativa do deploy hook (O4).** A O4 não diz se a chamada ao hook é feita uma única vez, como a O9 exige para a API do Render, nem se a falha do hook traz uma mensagem com o código HTTP.
- **`SERVICE_URL` fora da O14.** O smoke test e o rollback leem `secrets.SERVICE_URL`, que não está na lista de segredos do environment `production`. Decidir se ele vai para o environment ou fica como segredo do repositório. A inclusão de `RENDER_DEPLOY_HOOK_URL` na lista também foi inferida de "segredos de deploy".
- **Como o job aparece quando o environment `production` o bloqueia (O14, CA52).** A spec não diz se ele aparece como falha, como "pulado" ou como bloqueado.
- **ADR própria para o environment `production`.** A guarda dos segredos num environment restrito à `main` foi registrada como consequência na ADR 0013, já aceita. Decidir se vira ADR nova, conforme o CLAUDE.md.
- **Limite exato do prazo (O13).** A spec não diz se uma consulta que começa exatamente no fim do prazo vale, nem quando a primeira consulta começa depois do 2xx.
- **Valores de intervalo e prazo nos testes (O13).** A spec não diz o nome das variáveis de ambiente nem se o CA43 e o CA44 são testados com os tempos reais ou reduzidos. A espera na fila do CA44 não acontece num teste do script.
- **"Ainda não" sem prova direta (CA47 a CA51).** Os CAs de `commit` nulo, numérico, vazio, não JSON e `"desconhecido"` só testam a falha no fim do prazo. Nenhum mostra um desses casos seguido de sucesso, como o CA41 faz para campo ausente.
- **Deploy alcançado por commit mais novo (O4).** Se outro merge entrar antes de o deploy terminar, o Render pode publicar um commit mais novo. Nesse caso, o smoke test espera o SHA anterior e falha por prazo. A spec não define esse caso.
- **Status das ADRs desta revisão.** A ADR 0009, já aceita, foi editada sem nota de revisão. A ADR 0013, também aceita, foi editada de novo na PR `docs/rollback-spec-adr-0013` (nomes dos segredos), sem nota de revisão. Decidir se uma ADR aceita ganha nota quando é editada.
