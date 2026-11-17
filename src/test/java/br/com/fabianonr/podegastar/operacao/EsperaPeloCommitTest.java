package br.com.fabianonr.podegastar.operacao;

import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.fecharSemResposta;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.healthComCommit;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.healthComCommitDepoisDe;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.respostaSemTls;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Espera pelo commit no /health, comum ao smoke test e ao rollback (O10, O13). */
class EsperaPeloCommitTest {

  private static final String SHA_ESPERADO = "0123456789abcdef0123456789abcdef01234567";
  private static final String OUTRO_COMMIT = "fedcba9876543210fedcba9876543210fedcba98";
  private static final Duration TOLERANCIA = Duration.ofMillis(300);

  @Test
  @DisplayName(
      "Falha de DNS conta como ainda não e a espera falha por prazo com o SHA esperado (O10)")
  void falhaDeDnsContaComoAindaNao() {
    var resultado = esperar("http://servico-inexistente.invalid");

    assertFalhaPorPrazoDepoisDeMaisDeUmaConsulta(resultado);
  }

  @Test
  @DisplayName(
      "Falha de TLS conta como ainda não e a espera falha por prazo com o SHA esperado (O10)")
  void falhaDeTlsContaComoAindaNao() throws Exception {
    try (var servicoSemTls = ServidorHttpFalso.comRoteiro(respostaSemTls())) {
      var resultado = esperar("https://127.0.0.1:" + servicoSemTls.porta());

      assertFalhaPorPrazoDepoisDeMaisDeUmaConsulta(resultado);
      assertThat(servicoSemTls.pedidos()).hasSizeGreaterThan(1);
    }
  }

  @Test
  @DisplayName(
      "Conexão recusada em todas as consultas faz a espera falhar por prazo com o SHA esperado (O10)")
  void conexaoRecusadaContaComoAindaNao() {
    var resultado = esperar("http://127.0.0.1:" + ServidorHttpFalso.portaLivre());

    assertFalhaPorPrazoDepoisDeMaisDeUmaConsulta(resultado);
  }

  @Test
  @DisplayName("Conexão interrompida conta como ainda não e a espera continua (O10)")
  void conexaoInterrompidaContaComoAindaNao() throws Exception {
    try (var servico =
        ServidorHttpFalso.comRoteiro(fecharSemResposta(), healthComCommit(SHA_ESPERADO))) {
      var resultado = esperar(servico.endereco());

      assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
      assertThat(servico.pedidos()).hasSize(2);
    }
  }

  @Test
  @DisplayName("Nenhuma consulta nova começa depois do fim do prazo (O13)")
  void nenhumaConsultaNovaComecaDepoisDoPrazo() throws Exception {
    try (var servico = ServidorHttpFalso.comRoteiro(healthComCommit(OUTRO_COMMIT))) {
      var intervaloQueNaoDivideOPrazo = 2;
      var resultado =
          ExecucaoDeScript.comAmbienteDeTeste()
              .comIntervaloEmSegundos(intervaloQueNaoDivideOPrazo)
              .rodarFuncao("esperar_commit", servico.endereco(), SHA_ESPERADO);

      assertThat(resultado.terminouComSucesso()).isFalse();
      var consultas = servico.pedidos();
      assertThat(consultas).hasSize(2);
      var inicioDaEspera = consultas.getFirst().inicioEmNanos();
      var prazo = Duration.ofSeconds(ExecucaoDeScript.PRAZO_DE_TESTE_EM_SEGUNDOS);
      assertThat(consultas)
          .allSatisfy(consulta ->
              assertThat(Duration.ofNanos(consulta.inicioEmNanos() - inicioDaEspera))
                  .isLessThanOrEqualTo(prazo.plus(TOLERANCIA)));
    }
  }

  @Test
  @DisplayName("Consulta iniciada antes do fim do prazo vale mesmo que termine depois dele (O13)")
  void consultaIniciadaAntesDoPrazoValeMesmoTerminandoDepois() throws Exception {
    try (var servico =
        ServidorHttpFalso.comRoteiro(
            healthComCommit(OUTRO_COMMIT),
            healthComCommit(OUTRO_COMMIT),
            healthComCommitDepoisDe(Duration.ofSeconds(2), SHA_ESPERADO))) {
      var resultado = esperar(servico.endereco());

      assertThat(resultado.terminouComSucesso()).as(resultado.saida()).isTrue();
      assertThat(resultado.duracao())
          .isGreaterThan(Duration.ofSeconds(ExecucaoDeScript.PRAZO_DE_TESTE_EM_SEGUNDOS));
    }
  }

  private static ExecucaoDeScript.Resultado esperar(String enderecoDoServico) {
    return ExecucaoDeScript.comAmbienteDeTeste()
        .rodarFuncao("esperar_commit", enderecoDoServico, SHA_ESPERADO);
  }

  private static void assertFalhaPorPrazoDepoisDeMaisDeUmaConsulta(
      ExecucaoDeScript.Resultado resultado) {
    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida())
        .contains("Consulta 1")
        .contains("Consulta 2")
        .contains("Prazo")
        .contains(SHA_ESPERADO);
  }
}
