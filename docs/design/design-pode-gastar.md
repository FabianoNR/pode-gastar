# Design: Pode Gastar, decisão de pode ou não pode

Congelado em 20/11/2026: regra vigente na spec (docs/spec.md). Este documento é histórico.

Status: aprovado pelo Fabiano em 11/11/2026. Os acréscimos de 16/11/2026 (seção própria) foram aprovados pelo Fabiano em 16/11/2026.
Substitui qualquer entendimento anterior sobre o escopo de decisão. A spec correspondente é docs/spec.md (v8).

## Problema

Durante o mês, o Fabiano quer saber se uma compra cabe no orçamento sem tirar dinheiro de itens que já planejou. Se não couber, quer saber por quê. Se for possível couber cortando itens, quer ver quais sairiam, com a menor perda.

## Entendimento aprovado

### Modelo do mês
- Renda: entrada do mês.
- Custo fixo: contas recorrentes, conhecidas no início do mês. Variações no meio do mês são raras e pequenas.
- Investimento: decidido no início do mês. Status PLANEJADO (ainda pode mudar) ou APLICADO (saiu da conta, não volta atrás). Fica separado do custo fixo.
- Orçamento variável: renda, menos custo fixo, menos investimento.
- Custo variável: gastos que não são fixos nem investimento. Podem ser planejados (definidos no início do mês) ou não planejados (surgem durante o mês).
- Cartão de crédito: item de custo variável planejado, categoria devo, intensidade 5. Nunca é cortado.

### Status dos itens de custo variável
- PENDENTE: planejado, sem gasto. Reserva o valor estimado.
- EM_DISCUSSAO: não planejado, solicitado durante o mês, ainda não decidido. Reserva o valor estimado.
- PARCIAL: já houve gasto, mas ainda vão acontecer outros. Reserva o que falta do planejado, ou seja, o maior entre o estimado e o realizado.
- REALIZADO: encerrado. Vale o valor realizado.

### Dois cálculos
- Total restante: orçamento variável, menos o que já saiu (realizado de PARCIAL e REALIZADO).
- Livre: orçamento variável, menos o que está comprometido (estimado de PENDENTE e EM_DISCUSSAO, maior entre estimado e realizado para PARCIAL, realizado para REALIZADO).

### Decisão para uma compra
- Compra com valor e categoria. Não vira item do mês.
- APROVADO: cabe no livre. Mostra o saldo restante.
- CABE_COM_CORTES: não cabe no livre, mas cabe no total restante. Mostra a falta e as sugestões de corte.
- NAO_CABE: maior que o total restante. Cortar não resolve, porque cortar não altera o total restante. Mostra o motivo e a falta. Exceção: compra seria legal sem itens suficientes; a falta é o quanto ainda falta depois de cortar todos os elegíveis (ver Acréscimos de 16/11/2026).

### Sugestões de corte
- Prioridade de corte, do menos importante para o mais importante: primeiro os itens EM_DISCUSSAO, de qualquer categoria (entre eles, categoria seria legal, depois quero, depois devo, e só depois intensidade, menor primeiro); depois seria legal; depois quero; depois devo. Em cada grupo entram itens PENDENTE ou PARCIAL.
- Regra de corte: corta primeiro o menos importante; do mais importante, só o mínimo que faltar. Do grupo mais importante entra só a combinação de menor valor que cobre o que os grupos menos importantes, inteiros, não conseguem cobrir; depois o mesmo para o grupo anterior. Empate: menor soma de intensidades, depois menor id. Nenhum item entra sem ser necessário.
- Compra seria legal: entram itens em discussão e itens seria legal; quero e devo pendentes ou parciais ficam de fora. Se não bastar, a resposta é não, com motivo, e a falta é o quanto ainda faltaria depois de cortar todos os elegíveis.
- Compra quero: em discussão, seria legal, quero e, como último recurso, devo pendente ou parcial. Se cobre, uma sugestão. Se nem tudo junto cobre, mostra a lista completa e o quanto ainda falta.
- Compra devo, passo a passo. Primeiro, a regra de corte só com os itens em discussão, seria legal e quero; se cobre, termina, com uma sugestão. Se não cobre, são três opções, e em todas entram todos os itens em discussão e todos os seria legal. (1) Mais todos os quero e um devo pendente ou parcial que feche o que falta. (2) Mais a combinação de quero com devo que fecha o que falta, de menor soma de intensidades. (3) Mais um devo pendente ou parcial que feche o que falta. Quando uma opção escolhe um devo, vale menor intensidade, depois menor valor, depois menor id. Opções com os mesmos itens valem uma só.
- As três opções são alternativas para a pessoa escolher entre cortar mais quero ou sacrificar um devo. Por definição, levam todos os itens em discussão e todos os seria legal.
- Se nenhuma opção cobrir, mostra a lista completa e o quanto ainda falta.
- Lista completa: primeiro os itens em discussão; depois seria legal, quero e devo; dentro de cada categoria, menor intensidade primeiro; empate pelo id. Só aparece quando a soma de todos os itens elegíveis não cobre a falta.
- Itens REALIZADOS nunca são sugeridos. O cartão nunca é sugerido.
- Item PARCIAL pode ser cortado: o corte cancela só o restante (planejado menos realizado), e o item vira REALIZADO com o valor já gasto. PARCIAL que já gastou além do previsto não entra, porque cortar não libera valor.
- Itens EM_DISCUSSAO trazem o decisor responsável, na mensagem "aguardando decisão de…".

### Papéis
- Decisor: pode cortar qualquer custo. Quando reprova um item em discussão, ele sai do mês.
- Membros não decisores: podem solicitar itens. A aprovação e a remoção passam por um circuito próprio, fora desta fase.
- Solicitante não entra na decisão.

## Decisões de arquitetura
- Arquitetura hexagonal com DDD. O domínio não conhece Spring nem JSON.
- Porta de entrada: o agregado Mes, lido de uma porta. Nesta fase, a porta é mockada em memória.
- Banco de dados e repositório real: depois.
- O mês fica guardado no servidor. A consulta é GET /meses/{mesId}/avaliacao, com valor e categoria. O nome não entra na consulta.
- Front web responsivo em Vue + Vite, em repositório separado da API. Mobile fica para depois, usando a mesma API.
- Testes de arquitetura (ArchUnit) são desejáveis, mas são dependência nova, e foram autorizados em 11/11/2026.

## Acréscimos da v7 (12/11/2026)

Entendimento que sustenta a spec v7. Não repete a spec: registra o que ficou decidido sobre o modelo e sobre quem faz o quê.

### Identidade do item
- Cada item tem um id interno, único dentro do mês.
- O nome serve só para exibição.
- Itens com o mesmo nome são itens diferentes.

### Nomes repetidos
- Quando há nomes repetidos no mês, a resposta da avaliação traz avisos.
- Nenhum item é removido ou escondido por causa disso.
- A comparação de nomes ignora maiúsculas e espaços no início e no fim.

### Quem monta as mensagens
- Os textos de negócio (motivo, avisos e a mensagem de item em discussão) são montados pelo serviço de aplicação e vêm prontos na resposta.
- A tela só exibe.
- Os erros de formato da requisição (valor inválido, campo obrigatório) continuam no adaptador web.

### Ordem das combinações
- Menor soma de intensidades.
- Em empate, menor soma de valores.
- Em empate, o id.

### Compra seria legal
- Só entram no cálculo os itens seria legal e os itens em discussão.

## Acréscimos de 16/11/2026

Decisões do Fabiano em 16/11/2026, sobre a v7. Sustentam a spec v8.

### Regra de corte
- Corta primeiro o menos importante; do mais importante, só o mínimo que faltar.
- Ordem de importância: em discussão, seria legal, quero, devo.
- A conta vai do grupo mais importante para o menos importante: de cada grupo entra só a combinação de menor valor que cobre o que os grupos menos importantes, inteiros, não cobrem.
- Empate: menor soma de intensidades, depois menor id.
- Item pendente ou em discussão sai inteiro; item parcial libera só o restante (planejado menos realizado).
- Substitui a ordem da v7 dentro do grupo (intensidade antes do valor): agora o valor vem antes, para cortar só o necessário.

### Compra quero
- Seria legal e quero não cobrem: o devo pendente ou parcial entra como último grupo, pela mesma regra de corte.
- Se cobre, uma sugestão. Se nem tudo junto cobre, lista completa e quanto ainda falta, com o motivo atual.

### Compra devo
- Primeiro, a regra de corte com em discussão, seria legal e quero. Se cobre, termina.
- Se não cobre, as opções 1, 2 e 3, nessa ordem. Elas são alternativas e, por definição, levam todos os itens em discussão e todos os seria legal.
- Devo parcial entra nas três opções, junto com devo pendente, pelo valor cortável.
- A escolha de um devo usa menor intensidade, depois menor valor, depois menor id, nas opções 1 e 3. A opção 2 usa menor soma de intensidades, depois de valores, depois id.
- Opções com os mesmos itens valem uma só.

### Lista completa
- Ordem: em discussão; depois seria legal, quero e devo; dentro de cada categoria, menor intensidade primeiro; empate pelo id.
- Só aparece quando a soma de todos os itens elegíveis não cobre a falta, então o que ainda faltaria é sempre maior que zero.

### Item parcial
- Entra no grupo da categoria dele, junto com os pendentes.
- Se já gastou além do previsto, cortar não libera valor: não entra.

### Falta no não cabe por seria legal
- Quando a compra seria legal não tem itens suficientes, a falta é o quanto ainda faltaria depois de cortar todos os elegíveis.

### Mantidos
- 6b: nenhuma opção cobre a falta; lista completa e quanto ainda falta, com o motivo atual.
- Limite de 20 itens (6a): adiado. Continua valendo por grupo.
- Cartão de crédito reconhecido pelo nome: pendência registrada na spec.

## Fora de escopo nesta fase
- Grupo, identificação de usuário, papéis e permissões.
- Fluxo de solicitação, avaliação, aprovação e reprovação de itens.
- Cadastro de itens durante o mês, inclusive não planejados.
- Telas de registro de gasto e de atualização de status.
- Atualização de custo fixo no meio do mês.
- Ajuste de investimento PLANEJADO.
- Persistência, autenticação e aplicativo mobile.
