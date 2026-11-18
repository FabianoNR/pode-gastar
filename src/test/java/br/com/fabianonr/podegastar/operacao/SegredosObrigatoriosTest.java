package br.com.fabianonr.podegastar.operacao;

import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.healthComCommit;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.responder;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Segredos obrigatórios do deploy normal e do rollback (O15). */
class SegredosObrigatoriosTest {

  private static final String SHA = "0123456789abcdef0123456789abcdef01234567";
  private static final String CHAVE_DA_API = "chave-secreta-de-teste-91";
  private static final String ID_DO_SERVICO = "srv-secreto-de-teste-37";
  private static final String CHAVE_DO_DEPLOY_HOOK = "chave-do-hook-de-teste-55";

  /** Valores que contam como segredo ausente; vazio indica a variável não definida. */
  private static final List<Optional<String>> VALORES_AUSENTES =
      List.of(Optional.empty(), Optional.of(""), Optional.of("   "), Optional.of(" \t\n "));

  private ServidorHttpFalso deployHookOuApi;
  private ServidorHttpFalso servico;

  @BeforeEach
  void subirServidores() {
    deployHookOuApi = ServidorHttpFalso.comRoteiro(responder(201, "{}"));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit(SHA));
  }

  @AfterEach
  void fecharServidores() throws Exception {
    deployHookOuApi.close();
    servico.close();
  }

  static Stream<Arguments> segredosAusentesDoDeploy() {
    return combinar(List.of("RENDER_DEPLOY_HOOK_URL", "SERVICE_URL"));
  }

  static Stream<Arguments> segredosAusentesDoRollback() {
    return combinar(List.of("RENDER_API_KEY", "RENDER_SERVICE_ID", "SERVICE_URL"));
  }

  private static Stream<Arguments> combinar(List<String> segredos) {
    return segredos.stream()
        .flatMap(segredo -> VALORES_AUSENTES.stream().map(valor -> Arguments.of(segredo, valor)));
  }

  @ParameterizedTest(name = "{0} = {1}")
  @MethodSource("segredosAusentesDoDeploy")
  @DisplayName(
      "Deploy normal com segredo ausente, vazio ou só com espaços falha no início, antes de qualquer chamada HTTP (CA52, CA54, CA56)")
  void deployComSegredoAusenteFalhaAntesDeQualquerChamada(
      String segredoAusente, Optional<String> valor) {
    var execucao =
        ExecucaoDeScript.comAmbienteDeTeste()
            .comVariavel("RENDER_DEPLOY_HOOK_URL", urlDoDeployHook())
            .comVariavel("SERVICE_URL", servico.endereco());
    deixarAusente(execucao, segredoAusente, valor);

    var resultado = execucao.rodarScript("deploy.sh", SHA);

    assertFalhaPeloSegredo(resultado, segredoAusente);
  }

  @ParameterizedTest(name = "{0} = {1}")
  @MethodSource("segredosAusentesDoRollback")
  @DisplayName(
      "Rollback com segredo ausente, vazio ou só com espaços falha no início, antes de qualquer chamada HTTP (CA53, CA54, CA55, CA56)")
  void rollbackComSegredoAusenteFalhaAntesDeQualquerChamada(
      String segredoAusente, Optional<String> valor) {
    var execucao = execucaoDoRollbackComOsSegredos();
    deixarAusente(execucao, segredoAusente, valor);

    var resultado = execucao.rodarFuncao("executar_rollback", deployHookOuApi.endereco(), SHA);

    assertFalhaPeloSegredo(resultado, segredoAusente);
  }

  @Test
  @DisplayName("No rollback, a checagem dos segredos vem antes da validação do SHA (O15)")
  void rollbackChecaOsSegredosAntesDeValidarOSha() {
    var resultado =
        execucaoDoRollbackComOsSegredos()
            .comVariavel("RENDER_API_KEY", "")
            .rodarFuncao("executar_rollback", deployHookOuApi.endereco(), "sha-invalido");

    assertFalhaPeloSegredo(resultado, "RENDER_API_KEY");
    assertThat(resultado.saida()).doesNotContain("40 caracteres hexadecimais");
  }

  private ExecucaoDeScript execucaoDoRollbackComOsSegredos() {
    return ExecucaoDeScript.comAmbienteDeTeste()
        .comVariavel("RENDER_API_KEY", CHAVE_DA_API)
        .comVariavel("RENDER_SERVICE_ID", ID_DO_SERVICO)
        .comVariavel("SERVICE_URL", servico.endereco());
  }

  private String urlDoDeployHook() {
    return deployHookOuApi.endereco() + "/deploy/srv-hook?key=" + CHAVE_DO_DEPLOY_HOOK;
  }

  private static void deixarAusente(
      ExecucaoDeScript execucao, String segredo, Optional<String> valor) {
    valor.ifPresentOrElse(
        valorAusente -> execucao.comVariavel(segredo, valorAusente),
        () -> execucao.semVariavel(segredo));
  }

  private void assertFalhaPeloSegredo(ExecucaoDeScript.Resultado resultado, String segredo) {
    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida())
        .contains(segredo)
        .doesNotContain(CHAVE_DA_API)
        .doesNotContain(ID_DO_SERVICO)
        .doesNotContain(CHAVE_DO_DEPLOY_HOOK)
        .doesNotContain(deployHookOuApi.endereco())
        .doesNotContain(servico.endereco());
    assertThat(deployHookOuApi.pedidos()).isEmpty();
    assertThat(servico.pedidos()).isEmpty();
  }
}
