package br.com.fabianonr.podegastar.operacao;

import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.fecharSemResposta;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.healthComCommit;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.healthComCommitDepoisDe;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.informativoEFechar;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.redirecionarPara;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.responder;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.responderDepoisDe;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Rollback pela API do Render: pedido de deploy do commit e espera pelo /health (O5 a O10). */
class RollbackPelaApiDoRenderTest {

  private static final String SHA_PEDIDO = "0123456789abcdef0123456789abcdef01234567";
  private static final String COMMIT_ANTERIOR = "fedcba9876543210fedcba9876543210fedcba98";
  private static final String CHAVE_DA_API = "chave-de-teste";
  private static final String ID_DO_SERVICO = "srv-teste";
  private static final Duration TOLERANCIA = Duration.ofMillis(700);

  private ServidorHttpFalso apiDoRender;
  private ServidorHttpFalso servico;

  @AfterEach
  void fecharServidores() throws Exception {
    if (apiDoRender != null) {
      apiDoRender.close();
    }
    if (servico != null) {
      servico.close();
    }
  }

  @Test
  @DisplayName(
      "Rollback faz POST em /v1/services/{serviceId}/deploys com commitId e a chave da API (CA24)")
  void rollbackPedeODeployDoCommitAutenticado() throws Exception {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(201, "{}"));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PEDIDO));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(apiDoRender.pedidos()).hasSize(1);
    var pedido = apiDoRender.pedidos().getFirst();
    assertThat(pedido.metodo()).isEqualTo("POST");
    assertThat(pedido.caminho()).isEqualTo("/v1/services/" + ID_DO_SERVICO + "/deploys");
    assertThat(pedido.cabecalho("Authorization")).isEqualTo("Bearer " + CHAVE_DA_API);
    assertThat(pedido.cabecalho("Content-Type")).startsWith("application/json");
    var corpo = new ObjectMapper().readTree(pedido.corpo());
    assertThat(corpo.get("commitId").asText()).isEqualTo(SHA_PEDIDO);
  }

  @Test
  @DisplayName(
      "Rollback termina com sucesso na consulta que expõe o SHA pedido e não consulta mais (CA26)")
  void rollbackTerminaNaConsultaQueExpoeOShaPedido() {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(201, "{}"));
    servico =
        ServidorHttpFalso.comRoteiro(
            healthComCommit(COMMIT_ANTERIOR),
            healthComCommit(COMMIT_ANTERIOR),
            healthComCommit(SHA_PEDIDO));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(servico.pedidos()).hasSize(3);
  }

  @ParameterizedTest(name = "API responde {0}")
  @ValueSource(ints = {200, 202, 204})
  @DisplayName("Qualquer resposta 2xx da API aceita o pedido e o rollback espera o /health (CA32)")
  void qualquerResposta2xxAceitaOPedido(int status) {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(status, ""));
    servico =
        ServidorHttpFalso.comRoteiro(healthComCommit(COMMIT_ANTERIOR), healthComCommit(SHA_PEDIDO));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(servico.pedidos()).hasSize(2);
  }

  @Test
  @DisplayName(
      "Resposta 202 sem o SHA no /health dentro do prazo faz o rollback falhar por prazo (CA27)")
  void resposta202SemOShaNoPrazoFalhaPorPrazo() {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(202, "{}"));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(COMMIT_ANTERIOR));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertFalhaPorPrazo(resultado);
  }

  @ParameterizedTest(name = "API responde {0}")
  @ValueSource(ints = {400, 401, 404, 500, 503})
  @DisplayName(
      "Resposta 4xx ou 5xx da API faz o rollback falhar na hora com o código HTTP (CA28, CA29)")
  void resposta4xxOu5xxFalhaNaHoraComOCodigo(int status) {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(status, "{}"));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PEDIDO));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida()).contains("HTTP " + status);
    assertThat(apiDoRender.pedidos()).hasSize(1);
    assertThat(servico.pedidos()).isEmpty();
  }

  @ParameterizedTest(name = "API responde {0}")
  @ValueSource(ints = {301, 302, 307})
  @DisplayName("Resposta 3xx da API faz o rollback falhar na hora, sem seguir o Location (CA33)")
  void resposta3xxFalhaSemSeguirORedirecionamento(int status) throws Exception {
    try (var destinoDoRedirecionamento = ServidorHttpFalso.comRoteiro(responder(201, "{}"))) {
      apiDoRender =
          ServidorHttpFalso.comRoteiro(
              redirecionarPara(status, destinoDoRedirecionamento.endereco() + "/outro"));
      servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PEDIDO));

      var resultado = rodarRollback(SHA_PEDIDO);

      assertThat(resultado.terminouComSucesso()).isFalse();
      assertThat(resultado.saida()).contains("HTTP " + status);
      assertThat(destinoDoRedirecionamento.pedidos()).isEmpty();
      assertThat(servico.pedidos()).isEmpty();
    }
  }

  @Test
  @DisplayName(
      "Conexão recusada pela API faz o rollback falhar na hora, sem consultar o /health (CA34)")
  void conexaoRecusadaPelaApiFalhaNaHora() {
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PEDIDO));
    var apiSemNinguemEscutando = "http://127.0.0.1:" + ServidorHttpFalso.portaLivre();

    var resultado =
        execucaoDoRollback().rodarFuncao("executar_rollback", apiSemNinguemEscutando, SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida()).contains("não teve resposta");
    assertThat(servico.pedidos()).isEmpty();
  }

  @Test
  @DisplayName(
      "Conexão fechada pela API sem resposta faz o rollback falhar depois de uma única tentativa (CA34)")
  void conexaoFechadaSemRespostaFalhaDepoisDeUmaTentativa() {
    apiDoRender = ServidorHttpFalso.comRoteiro(fecharSemResposta());
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PEDIDO));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida()).contains("não teve resposta");
    assertThat(apiDoRender.pedidos()).hasSize(1);
    assertThat(servico.pedidos()).isEmpty();
  }

  @Test
  @DisplayName(
      "API que não responde dentro do tempo limite faz o rollback falhar na hora, depois de uma única tentativa (CA57)")
  void apiQueNaoRespondeNoTempoLimiteFalhaNaHora() {
    var limiteDaChamada = 2;
    apiDoRender =
        ServidorHttpFalso.comRoteiro(
            responderDepoisDe(Duration.ofSeconds(limiteDaChamada + 4), 201, "{}"));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PEDIDO));

    var resultado =
        execucaoDoRollback()
            .comLimiteDaChamadaEmSegundos(limiteDaChamada)
            .rodarFuncao("executar_rollback", apiDoRender.endereco(), SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida()).contains("tempo limite");
    assertThat(resultado.duracao())
        .isGreaterThanOrEqualTo(Duration.ofSeconds(limiteDaChamada))
        .isLessThan(Duration.ofSeconds(limiteDaChamada + 2));
    assertThat(apiDoRender.pedidos()).hasSize(1);
    assertThat(servico.pedidos()).isEmpty();
  }

  @Test
  @DisplayName("Resposta 1xx da API sem resposta final faz o rollback falhar na hora (O9)")
  void resposta1xxSemRespostaFinalFalhaNaHora() {
    apiDoRender = ServidorHttpFalso.comRoteiro(informativoEFechar());
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA_PEDIDO));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida()).contains("não teve resposta");
    assertThat(apiDoRender.pedidos()).hasSize(1);
    assertThat(servico.pedidos()).isEmpty();
  }

  @Test
  @DisplayName(
      "Consulta ao /health que passa de 5 segundos conta como ainda não e o rollback continua (CA36)")
  void consultaQuePassaDoTempoLimiteContaComoAindaNao() {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(201, "{}"));
    servico =
        ServidorHttpFalso.comRoteiro(
            healthComCommitDepoisDe(Duration.ofSeconds(8), SHA_PEDIDO),
            healthComCommit(SHA_PEDIDO));

    var resultado =
        execucaoDoRollback()
            .comPrazoEmSegundos(10)
            .rodarFuncao("executar_rollback", apiDoRender.endereco(), SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    var consultas = servico.pedidos();
    assertThat(consultas).hasSize(2);
    var esperaAteASegundaConsulta =
        Duration.ofNanos(consultas.get(1).inicioEmNanos() - consultas.get(0).inicioEmNanos());
    assertThat(esperaAteASegundaConsulta)
        .isBetween(
            Duration.ofSeconds(5 + ExecucaoDeScript.INTERVALO_DE_TESTE_EM_SEGUNDOS),
            Duration.ofSeconds(5 + ExecucaoDeScript.INTERVALO_DE_TESTE_EM_SEGUNDOS)
                .plus(TOLERANCIA));
  }

  @Test
  @DisplayName(
      "Toda consulta ao /health com 502 faz o rollback falhar por prazo com o SHA pedido (CA39)")
  void todaConsultaCom502FalhaPorPrazo() {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(201, "{}"));
    servico = ServidorHttpFalso.comRoteiro(responder(502, "Bad Gateway"));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertFalhaPorPrazo(resultado);
  }

  @Test
  @DisplayName("Resposta 200 sem campo commit conta como ainda não e o rollback continua (CA41)")
  void respostaSemCampoCommitContaComoAindaNao() {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(201, "{}"));
    servico =
        ServidorHttpFalso.comRoteiro(
            responder(200, "{\"status\": \"UP\"}"), healthComCommit(SHA_PEDIDO));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(servico.pedidos()).hasSize(2);
  }

  @Test
  @DisplayName("A consulta seguinte começa um intervalo depois do fim da anterior (CA43)")
  void intervaloContaDoFimDeUmaConsultaAoInicioDaProxima() {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(201, "{}"));
    servico =
        ServidorHttpFalso.comRoteiro(
            healthComCommitDepoisDe(Duration.ofSeconds(2), COMMIT_ANTERIOR),
            healthComCommit(SHA_PEDIDO));

    var resultado =
        execucaoDoRollback()
            .comPrazoEmSegundos(10)
            .rodarFuncao("executar_rollback", apiDoRender.endereco(), SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    var consultas = servico.pedidos();
    var intervalo = Duration.ofSeconds(ExecucaoDeScript.INTERVALO_DE_TESTE_EM_SEGUNDOS);
    var doFimDaPrimeiraAoInicioDaSegunda =
        Duration.ofNanos(consultas.get(1).inicioEmNanos() - consultas.get(0).fimEmNanos());
    assertThat(doFimDaPrimeiraAoInicioDaSegunda).isBetween(intervalo, intervalo.plus(TOLERANCIA));
  }

  @Test
  @DisplayName("O prazo começa na resposta 2xx da API, não no disparo do rollback (CA44)")
  void prazoComecaNaResposta2xxDaApi() {
    var esperaAteAResposta = Duration.ofSeconds(2);
    apiDoRender = ServidorHttpFalso.comRoteiro(responderDepoisDe(esperaAteAResposta, 201, "{}"));
    servico =
        ServidorHttpFalso.comRoteiro(
            healthComCommit(COMMIT_ANTERIOR),
            healthComCommit(COMMIT_ANTERIOR),
            healthComCommit(SHA_PEDIDO));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
    assertThat(resultado.duracao())
        .isGreaterThan(Duration.ofSeconds(ExecucaoDeScript.PRAZO_DE_TESTE_EM_SEGUNDOS));
  }

  @ParameterizedTest(name = "corpo {0}")
  @ValueSource(
      strings = {
        "{\"status\": \"UP\", \"commit\": null}",
        "{\"status\": \"UP\", \"commit\": 123}",
        "{\"status\": \"UP\", \"commit\": \"\"}",
        "UP",
        "{\"status\": \"UP\", \"commit\": \"desconhecido\"}"
      })
  @DisplayName(
      "Commit nulo, numérico, vazio, desconhecido ou corpo sem JSON contam como ainda não até o prazo (CA47, CA48, CA49, CA50, CA51)")
  void commitIlegivelContaComoAindaNaoAteOPrazo(String corpo) {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(201, "{}"));
    servico = ServidorHttpFalso.comRoteiro(responder(200, corpo));

    var resultado = rodarRollback(SHA_PEDIDO);

    assertFalhaPorPrazo(resultado);
    assertThat(servico.pedidos()).hasSizeGreaterThan(1);
  }

  private ExecucaoDeScript.Resultado rodarRollback(String sha) {
    return execucaoDoRollback().rodarFuncao("executar_rollback", apiDoRender.endereco(), sha);
  }

  private ExecucaoDeScript execucaoDoRollback() {
    return ExecucaoDeScript.comAmbienteDeTeste()
        .comVariavel("RENDER_API_KEY", CHAVE_DA_API)
        .comVariavel("RENDER_SERVICE_ID", ID_DO_SERVICO)
        .comVariavel("SERVICE_URL", servico.endereco());
  }

  private static void assertFalhaPorPrazo(ExecucaoDeScript.Resultado resultado) {
    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida()).contains("Prazo").contains(SHA_PEDIDO);
  }
}
