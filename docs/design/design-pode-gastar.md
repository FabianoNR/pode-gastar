# Design: Pode Gastar, decisão de pode ou não pode

Status: aprovado pelo Fabiano em 11/11/2026. Os acréscimos de 16/11/2026 (seção própria) foram aprovados pelo Fabiano em 16/11/2026.
Substitui qualquer entendimento anterior sobre o escopo de decisão. A spec correspondente é docs/spec.md (v8, em revisão).

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
- Ordem: primeiro os itens EM_DISCUSSAO, de qualquer categoria. Dentro deles, categoria (seria legal, depois quero, depois devo) e só depois intensidade (menor primeiro). Depois, os itens PENDENTE ou PARCIAL conforme a categoria da compra.
- Compra seria legal: entram itens seria legal e itens em discussão, de qualquer categoria; quero e devo pendentes ou parciais ficam de fora. Se não bastar, a resposta é não, com motivo.
- Compra quero: primeiro seria legal, depois quero; se ainda não bastar, devo pendente ou parcial, como terceiro grupo. Compra devo: primeiro seria legal, depois quero, e só então as opções de devo.
- Compra devo, passo a passo. Primeiro, tenta a combinação mínima só com os itens em discussão, seria legal e quero; se cobre, termina. Se não cobre, são três opções, e em todas entram todos os itens em discussão e todos os seria legal. (1) Mais todos os quero e um devo pendente ou parcial que feche o que falta. (2) Mais a combinação de quero com devo que fecha o que falta, de menor soma de intensidades. (3) Mais um devo pendente ou parcial que feche o que falta. Quando uma opção escolhe um devo, vale menor intensidade, depois menor valor, depois menor id. O corte do parcial cancela só o restante. Opções com os mesmos itens valem uma só.
- Se nenhuma opção cobrir, mostra a lista completa e o quanto ainda falta.
- Só o necessário, pela combinação mínima (compra seria legal e quero, e a primeira etapa da compra devo): a sugestão corta o menor valor total que cobre a falta. Os grupos são, nesta ordem, em discussão, seria legal, quero e devo (devo só na compra quero). Só entra o menor conjunto de grupos que, somados, cobrem a falta. Dentro dele vence a combinação de menor soma de cortes; no empate, a que corta mais dos grupos anteriores; depois a de menor soma de intensidades; depois a de menor id. Item pendente sai inteiro; item parcial libera só o restante.
- Compra quero sem cobertura por seria legal e quero: não usa as três opções. O devo pendente ou parcial entra como terceiro grupo. Se os três grupos cobrem a falta, uma sugestão. Se nem assim cobrem, mostra a lista completa e o quanto ainda falta, com o mesmo motivo (decisões de 16/11/2026, ver Acréscimos).
- Itens REALIZADOS nunca são sugeridos. O cartão nunca é sugerido.
- Item PARCIAL pode ser cortado: o corte cancela só o restante, e o item vira REALIZADO com o valor já gasto. PARCIAL que já gastou além do previsto não entra, porque cortar não libera valor.
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

### Compra quero sem cobertura
- Seria legal e quero não cobrem: o devo pendente ou parcial entra como terceiro grupo de corte.
- Se os três grupos cobrem a falta, uma sugestão.
- Se nem assim cobrem, lista completa e quanto ainda falta, com o motivo atual.

### Ordem da lista completa
- Primeiro os itens em discussão; depois seria legal, quero e devo; dentro de cada categoria, menor intensidade primeiro; empate pelo id.

### Item parcial
- Entra no grupo do pendente da categoria dele.
- Se já gastou além do previsto, cortar não libera valor: não entra.

### Falta no não cabe por seria legal
- Quando a compra seria legal não tem itens suficientes, a falta é o quanto ainda faltaria depois de cortar todos os elegíveis.

### Devo parcial nas opções da compra devo
- Devo parcial entra nas opções 1, 2 e 3, junto com devo pendente.
- O corte do parcial cancela só o restante: o valor cortável é o planejado menos o realizado.
- Quando uma das três opções escolhe entre devos, vale a mesma ordem dos demais itens: menor intensidade, depois menor valor, depois menor id.
- Consequência: lista completa só aparece quando a soma de todos os itens elegíveis não cobre a falta, então o que ainda faltaria é sempre maior que zero.

### Combinação mínima (só o necessário)
- Vale para a compra seria legal, para a compra quero e para a primeira etapa da compra devo.
- A sugestão corta o menor valor total que cobre a falta, não item a item.
- A ordem dos grupos (em discussão, seria legal, quero, devo) entra de dois jeitos: só entra o menor conjunto de grupos que, somados, cobre a falta; e, em empate de valor, vence a combinação que corta mais dos grupos anteriores.
- Depois do empate de grupos: menor soma de intensidades, depois menor id.
- Item pendente sai inteiro; item parcial libera só o restante.

### Compra devo, passo a passo
- Primeiro, a combinação mínima com os itens em discussão, seria legal e quero. Se cobre, termina.
- Se não cobre, as opções 1, 2 e 3, nessa ordem; em todas entram todos os itens em discussão e todos os seria legal. A escolha de um devo usa menor intensidade, depois menor valor, depois menor id, inclusive na opção 3.
- Opções com os mesmos itens valem uma só.
- Se nenhuma opção cobre, vale a 6b.

### Mantidos
- 6b: nenhuma opção cobre a falta; lista completa e quanto ainda falta, com o motivo atual.
- Limite de 20 itens (6a): adiado.

## Fora de escopo nesta fase
- Grupo, identificação de usuário, papéis e permissões.
- Fluxo de solicitação, avaliação, aprovação e reprovação de itens.
- Cadastro de itens durante o mês, inclusive não planejados.
- Telas de registro de gasto e de atualização de status.
- Atualização de custo fixo no meio do mês.
- Ajuste de investimento PLANEJADO.
- Persistência, autenticação e aplicativo mobile.
