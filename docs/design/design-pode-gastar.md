# Design: Pode Gastar, decisão de pode ou não pode

Status: aprovado pelo Fabiano em 11/11/2026.
Substitui qualquer entendimento anterior sobre o escopo de decisão. A spec correspondente é docs/spec.md (v6).

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
- NAO_CABE: maior que o total restante. Cortar não resolve, porque cortar não altera o total restante. Mostra o motivo e a falta.

### Sugestões de corte
- Ordem: primeiro os itens EM_DISCUSSAO, de qualquer categoria. Dentro deles, categoria (seria legal, depois quero, depois devo) e só depois intensidade (menor primeiro). Depois, os itens PENDENTE conforme a categoria da compra.
- Compra seria legal: só itens seria legal. Nunca sugere trocar por quero ou devo. Se não bastar, a resposta é não, com motivo.
- Compra quero ou devo: primeiro seria legal, depois quero.
- Devo PENDENTE: só como último recurso, em três opções. (1) todos os seria legal, todos os quero e um devo que feche o valor. (2) todos os seria legal e combinações de quero com devo, pelo menor intensidade. (3) todos os seria legal e um devo, por valor e intensidade baixa.
- Se nenhuma opção cobrir, mostra a lista completa e o quanto ainda falta.
- Itens REALIZADOS nunca são sugeridos. O cartão nunca é sugerido.
- Item PARCIAL pode ser cortado: o corte cancela só o restante, e o item vira REALIZADO com o valor já gasto.
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
- Testes de arquitetura (ArchUnit) são desejáveis, mas são dependência nova, e precisam de autorização.

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

## Fora de escopo nesta fase
- Grupo, identificação de usuário, papéis e permissões.
- Fluxo de solicitação, avaliação, aprovação e reprovação de itens.
- Cadastro de itens durante o mês, inclusive não planejados.
- Telas de registro de gasto e de atualização de status.
- Atualização de custo fixo no meio do mês.
- Ajuste de investimento PLANEJADO.
- Persistência, autenticação e aplicativo mobile.
