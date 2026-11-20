# Operação: deploy e rollback

Runbook da pipeline de entrega: deploy normal e rollback. Este documento descreve a pipeline, não o produto. As regras do `/health` (O1 a O3) e os critérios de aceite do `/health` (CA13, CA21, CA30 e CA31) ficam em [docs/spec.md](spec.md).

## Regras

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

- **O11.** O workflow de rollback só roda na `main`. O job tem uma condição que exige a `main`; disparado a partir de outra branch, o job não roda e aparece no GitHub como "pulado" (skipped), e nenhuma chamada é feita à API do Render nem ao `/health`. Esse é o comportamento esperado; a execução não aparece como falha. Um workflow editado em outra branch pode remover essa condição e rodar com os segredos do repositório; esse é um risco aceito pelo dono do repositório (ver "Riscos aceitos").
- **O12.** No `ci.yml`, o deploy hook e o smoke test formam um único job, em sequência: primeiro o deploy hook, depois o smoke test. O grupo de concorrência fica nesse job e tem o mesmo nome do grupo do job de rollback. Assim, o deploy normal e o rollback nunca rodam ao mesmo tempo, e nenhum rollback roda entre o deploy hook e o smoke test. Quem chega depois espera, sem cancelar o que já está rodando (`cancel-in-progress: false`). Se houver mais de uma execução esperando no grupo, o GitHub cancela a que estava na fila e mantém só a mais nova.

### Intervalo e prazo das consultas

- **O13.** O intervalo de 15 segundos conta do fim de uma consulta ao início da próxima. Nenhuma consulta nova começa depois do fim do prazo. Uma consulta iniciada antes do fim do prazo vale, mesmo que termine depois dele. A regra do prazo vale para o rollback e para o deploy normal: o prazo começa na resposta 2xx da API do Render (O7) ou do deploy hook (O4). Nos testes automatizados, o script de espera recebe o intervalo e o prazo por variável de ambiente, usada só nos testes; os workflows usam os valores desta seção.

### Segredos de deploy

- **O14.** Os segredos de deploy ficam nos segredos do repositório no GitHub, sem environment. O job de deploy hook e smoke test do `ci.yml` usa `RENDER_DEPLOY_HOOK_URL` e `SERVICE_URL`. O job de rollback usa `RENDER_API_KEY`, `RENDER_SERVICE_ID` e `SERVICE_URL`. `SERVICE_URL`, a URL base do serviço publicado, também é um segredo do repositório.

### Segredos obrigatórios

- **O15.** Todos os segredos da O14 são obrigatórios no fluxo que os usa: `RENDER_DEPLOY_HOOK_URL` e `SERVICE_URL` no deploy normal; `RENDER_API_KEY`, `RENDER_SERVICE_ID` e `SERVICE_URL` no rollback. Segredo ausente, vazio ou só com espaços em branco conta como ausente. Com um segredo ausente, o fluxo falha no início, antes de qualquer chamada HTTP (deploy hook, API do Render ou `/health`), com código de saída diferente de zero e uma mensagem que diz qual segredo falta. Nenhuma mensagem imprime o valor de um segredo. No rollback, a checagem dos segredos vem antes da validação do SHA (O6). No deploy normal, isso substitui o comportamento anterior do deploy hook vazio, que avisava e terminava com sucesso.

### Tempo limite das chamadas e dos jobs

- **O16.** Cada chamada à API do Render (O5) e ao deploy hook (O4) tem tempo limite de 30 segundos, que inclui a conexão e a leitura da resposta. Se a chamada passar desse tempo, o fluxo falha na hora, sem nova tentativa e sem consultar `/health`, com uma mensagem clara de que a chamada passou do tempo limite. Nos testes automatizados, esse tempo limite pode ser reduzido por variável de ambiente, usada só nos testes, como o intervalo e o prazo da O13; os workflows não definem essa variável, e o padrão continua 30 segundos. O job de deploy hook e smoke test do `ci.yml` e o job de rollback têm `timeout-minutes: 15`. A espera pelo `/health` não muda: 5 segundos por consulta (O10), 15 segundos entre consultas e prazo de 10 minutos (O7, O13).

### Esclarecimentos da operação

- **O17.** Estes pontos não mudam o comportamento das regras anteriores; só registram o que é aceito:
  - A mensagem de falha do deploy hook pode trazer o código HTTP da resposta. Isso é permitido, não exigido (O4).
  - A falha por prazo pode sair até um intervalo entre consultas (15 segundos) depois do fim do prazo de 10 minutos, porque o fim do prazo é conferido depois do intervalo. Isso é aceito e complementa a O13.
  - A comparação do `commit` com o SHA esperado usa o valor devolvido pelo `/health`, sem aparar de novo. A premissa é que o `/health` já devolve o valor aparado (O2) (docs/spec.md).

## Critérios de aceite

- **CA22.** Deploy normal do commit `<sha>`: depois do deploy hook, `/health` expõe outro commit nas primeiras consultas e `<sha>` numa consulta seguinte, dentro de 10 minutos. O smoke test passa nessa consulta e não consulta mais (O4).
- **CA23.** Deploy normal do commit `<sha>`: por 10 minutos, `/health` expõe outro commit ou `"desconhecido"`. O smoke test falha por prazo, com uma mensagem que traz o SHA esperado (O4).
- **CA24.** Rollback com um SHA de 40 caracteres hexadecimais minúsculos: o workflow faz um `POST` em `https://api.render.com/v1/services/{serviceId}/deploys` com o corpo `{"commitId": "<sha>"}`, autenticado pela chave da API do Render (O5).
- **CA25.** Rollback com um SHA de 39 caracteres, de 41 caracteres, com letra maiúscula ou com caractere fora de `0-9` e `a-f`: o workflow falha com uma mensagem clara sobre o formato do SHA, sem nenhuma chamada à API do Render nem ao `/health` (O6).
- **CA26.** Rollback para `<sha>`: a API responde 201 e `/health` expõe o commit anterior nas primeiras consultas e `<sha>` numa consulta seguinte, dentro de 10 minutos. O rollback termina com sucesso nessa consulta e não consulta mais (O7, O8).
- **CA27.** Rollback para `<sha>`: a API responde 202 e, por 10 minutos, `/health` não expõe `<sha>`. O rollback falha por prazo, com uma mensagem que traz o SHA pedido, mesmo com a resposta de sucesso da API (O7, O8).
- **CA28.** Rollback para `<sha>`: a API responde 401. O rollback falha na hora, com uma mensagem que traz o código 401, sem consultar `/health` (O9).
- **CA29.** Rollback para `<sha>`: a API responde 503. O rollback falha na hora, com uma mensagem que traz o código 503, sem consultar `/health` (O9).
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
- **CA52.** Deploy normal com `RENDER_DEPLOY_HOOK_URL` ausente ou vazio: o job falha no início, com código de saída diferente de zero e uma mensagem que cita `RENDER_DEPLOY_HOOK_URL`, sem chamar o deploy hook nem o `/health` (O15).
- **CA53.** Rollback para `<sha>` com `RENDER_API_KEY` ausente ou vazia: o rollback falha no início, com código de saída diferente de zero e uma mensagem que cita `RENDER_API_KEY`, sem chamar a API do Render nem o `/health` (O15).
- **CA54.** Deploy normal e rollback com `SERVICE_URL` ausente ou vazio: cada fluxo falha no início, com código de saída diferente de zero e uma mensagem que cita `SERVICE_URL`, sem chamar o deploy hook, a API do Render nem o `/health` (O15).
- **CA55.** Rollback para `<sha>` com `RENDER_SERVICE_ID` ausente ou vazio: o rollback falha no início, com código de saída diferente de zero e uma mensagem que cita `RENDER_SERVICE_ID`, sem chamar a API do Render nem o `/health` (O15).
- **CA56.** Deploy normal ou rollback com um segredo obrigatório só com espaços em branco: o fluxo trata o segredo como ausente e falha como nos CA52 a CA55, com uma mensagem que cita o segredo. Nenhuma mensagem dos CA52 a CA56 traz o valor de um segredo (O15).
- **CA57.** Rollback para `<sha>`: a API do Render aceita a conexão e não responde em 30 segundos. O rollback falha na hora, com uma mensagem clara de tempo limite, depois de uma única tentativa e sem consultar `/health` (O16).
- **CA58.** Deploy normal: o deploy hook aceita a conexão e não responde em 30 segundos. O job falha na hora, com uma mensagem clara de tempo limite, depois de uma única chamada e sem consultar `/health` (O16).
- **CA59.** O job de deploy hook e smoke test do `ci.yml` e o job de rollback do `rollback.yml` têm `timeout-minutes: 15`. Verificado por teste estático dos workflows (O16).

## Verificação manual

Estes passos usam uma execução real no GitHub e no Render, depois do merge da PR que implementa a seção "Operação". O CA37 usa o SHA que já está no ar e não troca a versão publicada. O CA38 é coberto pelos passos do CA42. O CA42 aproveita o merge de uma PR já aprovada, que publica uma versão nova pelo deploy normal.

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

## Riscos aceitos

- **Workflow editado em outra branch (O11), risco aceito.** A condição que limita o rollback à `main` fica no próprio `rollback.yml`. Um workflow editado em outra branch pode remover a condição e rodar com os segredos do repositório (O14). Risco aceito pelo dono do repositório em 17/11/2026.
- **Cancelamento cruzado na fila (O12), risco aceito.** Pelo grupo de concorrência, um deploy normal que chega depois cancela um rollback que estava na fila, e um rollback que chega depois cancela um deploy normal que estava na fila. Risco aceito pelo Fabiano em 17/11/2026. Mitigação: não mesclar na `main` enquanto um rollback estiver em andamento.
