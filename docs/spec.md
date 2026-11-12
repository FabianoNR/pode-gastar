# Spec: Pode Gastar (v7)

Status: v7 em revisão, ainda não aprovada nem mesclada na main. Substitui a v6. Escopo de decisão fechado. Grupo, identificação de usuário, permissões e aprovação ficam para as funcionalidades futuras. Mudanças futuras da spec vão em PR próprio, separado da implementação, conforme o CLAUDE.md.

## Histórico

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
- **R4.** Se a compra é maior que o total restante, a decisão é `NAO_CABE`. Cortar itens não resolve, porque cortar não altera o total restante. Motivo: "compra excede o total que ainda sobra no mês". Falta = compra − total restante.
- **R5.** Elegíveis para corte:
  - `EM_DISCUSSAO`, de qualquer categoria, incluindo `DEVO_FAZER`.
  - `PENDENTE` de `SERIA_LEGAL` e `QUERO_FAZER`, conforme R6.
  - `PARCIAL`. Cortar cancela só o restante. O item passa a `REALIZADO` com o valor já gasto.
  - `PENDENTE` de `DEVO_FAZER`, só como último recurso (R7).
  - Nunca `REALIZADO`. Nunca o cartão de crédito.
- **R6.** Ordem das sugestões:
  1. Itens `EM_DISCUSSAO`, de qualquer categoria. Dentro deles, a ordem é pela categoria (seria legal, depois quero, depois devo) e só depois pela intensidade (menor primeiro).
  2. Depois, conforme a categoria da compra:
     - Compra `SERIA_LEGAL`: só `SERIA_LEGAL` `PENDENTE` elegíveis. Nenhum `QUERO_FAZER` ou `DEVO_FAZER` `PENDENTE` é sugerido como troca. Se não bastarem, `NAO_CABE`, com motivo "não há itens seria legal suficientes para cobrir a compra". Ver R11.
     - Compra `QUERO_FAZER` ou `DEVO_FAZER`: primeiro `SERIA_LEGAL` `PENDENTE`; se não bastarem, `QUERO_FAZER` `PENDENTE`.
     - Compra `DEVO_FAZER` não coberta por todos os `SERIA_LEGAL` mais todos os `QUERO_FAZER`: aplica-se R7.
  - Dentro de cada grupo, as combinações que cobrem a falta são ordenadas, nesta ordem:
    1. menor soma das intensidades dos itens da combinação;
    2. em empate, menor soma dos valores dos itens;
    3. em empate, menor id, na ordem alfabética dos ids. O id é único no mês, então o resultado é sempre o mesmo.
  - Uma combinação pode ter quantos itens forem necessários.
- **R7.** Três opções para compra `DEVO_FAZER` sem cobertura:
  - **Opção 1:** todos os `SERIA_LEGAL`, todos os `QUERO_FAZER` e um `DEVO_FAZER` `PENDENTE` que feche o valor.
  - **Opção 2:** todos os `SERIA_LEGAL` e combinações de cortes (`QUERO_FAZER` com `DEVO_FAZER` `PENDENTE`) que somem o valor restante, começando pelos de menor intensidade.
  - **Opção 3:** todos os `SERIA_LEGAL` e um `DEVO_FAZER` `PENDENTE`, escolhido por valor e intensidade baixa, se cobrir a falta.
  - Se nenhuma opção cobrir a falta, a decisão continua `CABE_COM_CORTES`, com a lista completa de itens elegíveis e o valor que ainda faltaria.
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
- **200, `NAO_CABE`:** `totalRestante`, `falta` e `motivo`, sem sugestões.
- **400:** valor inválido ou categoria fora do conjunto. A mensagem aponta o campo.
- **404:** `mesId` não existe.
- **422:** grupo com mais de 20 itens ao calcular as sugestões. A resposta segue o formato dos demais erros (`campo`, quando se aplica, e `mensagem`), por exemplo `"itens demais para calcular sugestões de corte neste mês (máximo 20 por grupo)"`.

Cada item em `sugestoes` e em `listaCompleta` traz `id`, `nome`, `status` e, só para `EM_DISCUSSAO`, `decisorResponsavel` e `mensagem`.

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

`GET /health`: 200 com `{"status": "UP"}`.

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
- **CA5.** `valor=800.00&categoria=DEVO_FAZER`: falta 700. Opção 1 fecha exato cortando os `SERIA_LEGAL` e os `QUERO_FAZER`, sem `DEVO_FAZER`.
- **CA6.** `valor=1700.00&categoria=QUERO_FAZER`: `NAO_CABE`, motivo "compra excede o total que ainda sobra no mês", falta 100.
- **CA7.** Viagem `PARCIAL` com realizado de 150 (estimado 300): total restante 1450, livre 100. `valor=100.00&categoria=QUERO_FAZER`: `APROVADO`, saldo restante 0.
- **CA8.** Viagem `REALIZADO` com realizado de 250: total restante 1350, livre 150. `valor=150.00&categoria=QUERO_FAZER`: `APROVADO`, saldo restante 0.
- **CA9.** Caso CA8 com `valor=200.00&categoria=QUERO_FAZER`: falta 50, sugestão bola de futebol. A viagem, realizada, nunca aparece.
- **CA10.** Viagem `PARCIAL` com realizado de 150 e `valor=200.00&categoria=QUERO_FAZER` com livre insuficiente: a viagem é candidata a corte, cancelando só o restante de 150. Se for cortada, o item vira `REALIZADO` com valor 150.
- **CA11.** O cartão de crédito nunca aparece como sugestão de corte.
- **CA12.** `valor=0` ou negativo: 400.
- **CA13.** `GET /health`: 200 com `{"status": "UP"}`.

### Com a bolsa em discussão

- **CB1.** `valor=150.00&categoria=SERIA_LEGAL`: `CABE_COM_CORTES`, falta 140. Sugestão: bolsa (90), com "aguardando decisão de marido", mais bola de futebol (80). Somam 170 e sobram 30.
- **CB2.** `valor=80.00&categoria=QUERO_FAZER`: `CABE_COM_CORTES`, falta 70. Sugestão: bolsa, que sozinha cobre e sobra 20.
- **CB3.** A bolsa reclassificada como `DEVO_FAZER`, intensidade 2, ainda `EM_DISCUSSAO`: continua elegível e aparece primeiro na sugestão de `valor=150.00&categoria=SERIA_LEGAL`.
- **CB4.** Dois itens `EM_DISCUSSAO`, um `QUERO_FAZER` de intensidade 5 e um `DEVO_FAZER` de intensidade 2: o `QUERO_FAZER` vem antes na sugestão, porque a categoria pesa antes da intensidade.

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

## Perguntas em aberto

Nenhuma para o escopo de decisão. Como o `mesId` é obtido, por usuário ou por grupo, fica para a funcionalidade de grupo.

Pendência conhecida: o cartão de crédito (R5, CA11) ainda é reconhecido pelo nome. Enquanto isso, qualquer item com o nome "Cartão de crédito" é tratado como o cartão. A troca para um marcador próprio do item fica para depois.