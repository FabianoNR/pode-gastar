package br.com.fabianonr.podegastar.operacao;

import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.fecharSemResposta;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.healthComCommit;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.informativoEFechar;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.redirecionarPara;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.responder;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.responderDepoisDe;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Deploy normal: deploy hook e espera pelo commit publicado no /health (O4, O10, O13). */
class DeployNormalTest {

  private static final String SHA_PUBLICADO = "0123456789abcdef0123456789abcdef01234567";
  private static final String COMMIT_ANTERIOR = "fedcba9876543210fedcba9876543210fedcba98";
  private static final String CAMINHO_DO_DEPLOY_HOOK = "/deploy/srv-teste?key=k";

  private ServidorHttpFalso deployHook;
  private ServidorHttpFalso servico;

  @AfterEach
  void fecharServidores() throws Exception {
    if (deployHook != null) {
      deployHook.close();
    }
    if (servico != null) {
      servico.close();
    }
  }

  @Test
  @DisplayName("Smoke test passa na consulta que expõe o SHA publicado e não consulta mais (CA22)")
  void smokeTestPassaNaConsultaQueExpoeOShaPublicado() {
    deployHook = ServidorHttpFalso.comRoteiro(responder(200, "{}"));
    servico =
        ServidorHttpFalso.comRoteiro(
            healthComCommit(COMMIT_ANTERIOR), healthComCommit(SHA_PUBLICADO));

    var resultado = rodarDeploy(servico.endereco());

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(deployHook.pedidos()).hasSize(1);
    assertThat(deployHook.pedidos().getFirst().metodo()).isEqualTo("POST");
    assertThat(servico.pedidos()).hasSize(2);
  }

  @Test
  @DisplayName("O deploy hook é chamado na URL configurada, sem imgURL (ADR 0014)")
  void deployHookEChamadoSemImgUrl() {
    deployHook = ServidorHttpFalso.comRoteiro(responder(200, "{}"));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PUBLICADO));

    var resultado = rodarDeploy(servico.endereco());

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(deployHook.pedidos().getFirst().caminho()).isEqualTo(CAMINHO_DO_DEPLOY_HOOK);
  }

  @ParameterizedTest(name = "deploy hook responde {0}")
  @ValueSource(ints = {201, 202, 204})
  @DisplayName("Qualquer resposta 2xx do deploy hook leva à espera pelo /health (O4)")
  void qualquerResposta2xxDoDeployHookLevaAEspera(int status) {
    deployHook = ServidorHttpFalso.comRoteiro(responder(status, ""));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PUBLICADO));

    var resultado = rodarDeploy(servico.endereco());

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(servico.pedidos()).hasSize(1);
  }

  @Test
  @DisplayName("Smoke test falha por prazo com o SHA esperado quando o /health não o expõe (CA23)")
  void smokeTestFalhaPorPrazoComOShaEsperado() {
    deployHook = ServidorHttpFalso.comRoteiro(responder(200, "{}"));
    servico =
        ServidorHttpFalso.comRoteiro(
            healthComCommit(COMMIT_ANTERIOR), healthComCommit("desconhecido"));

    var resultado = rodarDeploy(servico.endereco());

    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida()).contains("Prazo").contains(SHA_PUBLICADO);
  }

  @Test
  @DisplayName("Smoke test continua depois de um 502 e passa na consulta seguinte (CA35)")
  void smokeTestContinuaDepoisDeUm502() {
    deployHook = ServidorHttpFalso.comRoteiro(responder(200, "{}"));
    servico =
        ServidorHttpFalso.comRoteiro(responder(502, "Bad Gateway"), healthComCommit(SHA_PUBLICADO));

    var resultado = rodarDeploy(servico.endereco());

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(servico.pedidos()).hasSize(2);
  }

  @Test
  @DisplayName(
      "Smoke test continua depois de uma conexão recusada e passa na consulta seguinte (CA40)")
  void smokeTestContinuaDepoisDeConexaoRecusada() throws Exception {
    deployHook = ServidorHttpFalso.comRoteiro(responder(200, "{}"));
    var portaDoServico = ServidorHttpFalso.portaLivre();

    var intervaloLongo = 2;
    var deployEmAndamento =
        CompletableFuture.supplyAsync(() ->
            execucaoDoDeploy("http://127.0.0.1:" + portaDoServico)
                .comIntervaloEmSegundos(intervaloLongo)
                .rodarScript("deploy.sh", SHA_PUBLICADO));
    esperarFimDoAtendimentoDoDeployHook();
    Thread.sleep(Duration.ofMillis(500));
    servico = ServidorHttpFalso.naPorta(portaDoServico, healthComCommit(SHA_PUBLICADO));
    var resultado = deployEmAndamento.join();

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(resultado.saida()).contains("Consulta 1").contains("Consulta 2");
  }

  @Test
  @DisplayName("Deploy hook com 500 faz o job falhar na hora, sem consultar o /health (CA45)")
  void deployHookCom500FalhaNaHora() {
    deployHook = ServidorHttpFalso.comRoteiro(responder(500, "{}"));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PUBLICADO));

    var resultado = rodarDeploy(servico.endereco());

    assertFalhaNaHoraSemConsultarOHealth(resultado);
    assertThat(deployHook.pedidos()).hasSize(1);
  }

  @ParameterizedTest(name = "deploy hook responde {0}")
  @ValueSource(ints = {400, 404, 503})
  @DisplayName("Deploy hook com 4xx ou 5xx faz o job falhar na hora, sem consultar o /health (O4)")
  void deployHookCom4xxOu5xxFalhaNaHora(int status) {
    deployHook = ServidorHttpFalso.comRoteiro(responder(status, "{}"));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PUBLICADO));

    var resultado = rodarDeploy(servico.endereco());

    assertFalhaNaHoraSemConsultarOHealth(resultado);
    assertThat(deployHook.pedidos()).hasSize(1);
  }

  @ParameterizedTest(name = "deploy hook responde {0}")
  @ValueSource(ints = {301, 302, 307})
  @DisplayName("Deploy hook com 3xx faz o job falhar na hora, sem seguir o Location (O4)")
  void deployHookCom3xxFalhaSemSeguirORedirecionamento(int status) throws Exception {
    try (var destinoDoRedirecionamento = ServidorHttpFalso.comRoteiro(responder(200, "{}"))) {
      deployHook =
          ServidorHttpFalso.comRoteiro(
              redirecionarPara(status, destinoDoRedirecionamento.endereco() + "/outro"));
      servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PUBLICADO));

      var resultado = rodarDeploy(servico.endereco());

      assertFalhaNaHoraSemConsultarOHealth(resultado);
      assertThat(deployHook.pedidos()).hasSize(1);
      assertThat(destinoDoRedirecionamento.pedidos()).isEmpty();
    }
  }

  @Test
  @DisplayName(
      "Deploy hook que não responde dentro do tempo limite faz o job falhar na hora, depois de uma única chamada (CA58)")
  void deployHookQueNaoRespondeNoTempoLimiteFalhaNaHora() {
    var limiteDaChamada = 2;
    deployHook =
        ServidorHttpFalso.comRoteiro(
            responderDepoisDe(Duration.ofSeconds(limiteDaChamada + 4), 200, "{}"));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PUBLICADO));

    var resultado =
        execucaoDoDeploy(servico.endereco())
            .comLimiteDaChamadaEmSegundos(limiteDaChamada)
            .rodarScript("deploy.sh", SHA_PUBLICADO);

    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida()).contains("tempo limite");
    assertThat(resultado.duracao())
        .isGreaterThanOrEqualTo(Duration.ofSeconds(limiteDaChamada))
        .isLessThan(Duration.ofSeconds(limiteDaChamada + 2));
    assertThat(deployHook.pedidos()).hasSize(1);
    assertThat(servico.pedidos()).isEmpty();
  }

  @Test
  @DisplayName("Deploy hook com 1xx sem resposta final faz o job falhar na hora (O4)")
  void deployHookCom1xxSemRespostaFinalFalhaNaHora() {
    deployHook = ServidorHttpFalso.comRoteiro(informativoEFechar());
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PUBLICADO));

    var resultado = rodarDeploy(servico.endereco());

    assertFalhaNaHoraSemConsultarOHealth(resultado);
    assertThat(deployHook.pedidos()).hasSize(1);
  }

  @Test
  @DisplayName(
      "Deploy hook com conexão recusada faz o job falhar na hora, sem consultar o /health (CA46)")
  void deployHookComConexaoRecusadaFalhaNaHora() {
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PUBLICADO));
    var hookSemNinguemEscutando = "http://127.0.0.1:" + ServidorHttpFalso.portaLivre();

    var resultado =
        ExecucaoDeScript.comAmbienteDeTeste()
            .comVariavel("RENDER_DEPLOY_HOOK_URL", hookSemNinguemEscutando + "/deploy")
            .comVariavel("SERVICE_URL", servico.endereco())
            .rodarScript("deploy.sh", SHA_PUBLICADO);

    assertFalhaNaHoraSemConsultarOHealth(resultado);
  }

  @Test
  @DisplayName(
      "Deploy hook com conexão fechada sem resposta faz o job falhar depois de uma única chamada (O4)")
  void deployHookComConexaoFechadaFalhaDepoisDeUmaChamada() {
    deployHook = ServidorHttpFalso.comRoteiro(fecharSemResposta());
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PUBLICADO));

    var resultado = rodarDeploy(servico.endereco());

    assertFalhaNaHoraSemConsultarOHealth(resultado);
    assertThat(deployHook.pedidos()).hasSize(1);
  }

  @Test
  @DisplayName("O prazo do smoke test começa na resposta 2xx do deploy hook (O4, O13)")
  void prazoDoSmokeTestComecaNaResposta2xxDoDeployHook() {
    deployHook = ServidorHttpFalso.comRoteiro(responderDepoisDe(Duration.ofSeconds(2), 200, "{}"));
    servico =
        ServidorHttpFalso.comRoteiro(
            healthComCommit(COMMIT_ANTERIOR),
            healthComCommit(COMMIT_ANTERIOR),
            healthComCommit(SHA_PUBLICADO));

    var resultado = rodarDeploy(servico.endereco());

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(resultado.duracao())
        .isGreaterThan(Duration.ofSeconds(ExecucaoDeScript.PRAZO_DE_TESTE_EM_SEGUNDOS));
  }

  private ExecucaoDeScript.Resultado rodarDeploy(String enderecoDoServico) {
    return execucaoDoDeploy(enderecoDoServico).rodarScript("deploy.sh", SHA_PUBLICADO);
  }

  private ExecucaoDeScript execucaoDoDeploy(String enderecoDoServico) {
    return ExecucaoDeScript.comAmbienteDeTeste()
        .comVariavel("RENDER_DEPLOY_HOOK_URL", deployHook.endereco() + CAMINHO_DO_DEPLOY_HOOK)
        .comVariavel("SERVICE_URL", enderecoDoServico);
  }

  private void esperarFimDoAtendimentoDoDeployHook() throws InterruptedException {
    var limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    while (deployHook.pedidos().stream().noneMatch(pedido -> pedido.fimEmNanos() != 0)) {
      if (System.nanoTime() > limite) {
        throw new IllegalStateException("O deploy hook não foi chamado em 10 s.");
      }
      Thread.sleep(Duration.ofMillis(20));
    }
  }

  private void assertFalhaNaHoraSemConsultarOHealth(ExecucaoDeScript.Resultado resultado) {
    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida()).contains("deploy hook");
    assertThat(resultado.duracao())
        .isLessThan(Duration.ofSeconds(ExecucaoDeScript.INTERVALO_DE_TESTE_EM_SEGUNDOS));
    assertThat(servico.pedidos()).isEmpty();
  }
}
