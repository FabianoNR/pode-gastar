# 0014: Render construído do repositório em vez de imagem do GHCR

Status: aceita

Data: 2026-11-18

## Contexto

Até 18/11/2026, o serviço `pode-gastar-api` no Render era um serviço por imagem: o `ci.yml` gerava a imagem Docker, publicava no GHCR (`ghcr.io/fabianonr/pode-gastar`) e chamava o deploy hook com `imgURL` apontando para essa imagem. A ADR 0013 partiu da premissa de que o serviço já era construído a partir do repositório, mas não era.

Serviço por imagem tem dois problemas para as regras de operação, hoje em `docs/operacao.md`:

- O Render não define `RENDER_GIT_COMMIT` nele. O `/health` devolvia `"desconhecido"` (O2, O3), e o deploy da PR #25 falhou por prazo, porque o smoke test nunca viu o SHA publicado (O4).
- O rollback por `commitId` (O5) só funciona em serviço construído a partir do repositório.

Em 18/11/2026 o serviço foi trocado no painel do Render para Git Provider, branch `main`, runtime Docker e Auto-Deploy desligado. Um deploy manual do `a28a8af` mostrou esse commit no `/health`.

## Decisão

- O Render constrói a aplicação a partir do repositório, com o `Dockerfile` da raiz.
- O `ci.yml` deixa de gerar e publicar imagem. O job `publish` e o login no GHCR saem.
- O job `deploy` depende do `verify`, roda só em push na `main`, chama o deploy hook sem `imgURL` e espera o `/health` expor o `github.sha` (O4).
- O Auto-Deploy do Render fica desligado. Quem dispara o deploy é o `ci.yml`, depois do `verify`.

## Consequências

- O build passa a rodar no Render, no plano grátis, a cada deploy. O tempo de build entra no prazo de 10 minutos do smoke test (O4) e do rollback (O7).
- O GHCR deixa de ser usado no deploy. O pacote `ghcr.io/fabianonr/pode-gastar` fica como está, sem apagar.
- O deploy hook de um serviço construído do repositório faz o deploy do último commit da branch configurada, não de um commit escolhido. Se outro merge entrar na `main` antes do build, o `/health` mostra o commit mais novo e o smoke test do commit anterior falha por prazo.
- O item "Publicação no GHCR", que ficava nos Abertos da spec, fica resolvido por esta ADR.
- Substitui em parte a ADR 0013 (premissa do contexto e consequência sobre o GHCR).
