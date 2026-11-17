package br.com.fabianonr.podegastar.operacao;

import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.healthComCommit;
import static br.com.fabianonr.podegastar.operacao.ServidorHttpFalso.responder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Validação do SHA do rollback antes de qualquer chamada HTTP (O6). */
class ValidacaoDoShaDoRollbackTest {

  private ServidorHttpFalso apiDoRender;
  private ServidorHttpFalso servico;

  @BeforeEach
  void subirServidores() {
    apiDoRender = ServidorHttpFalso.comRoteiro(responder(201, "{}"));
    servico = ServidorHttpFalso.comRoteiro(healthComCommit("qualquer"));
  }

  @AfterEach
  void fecharServidores() throws Exception {
    apiDoRender.close();
    servico.close();
  }

  @ParameterizedTest(name = "sha \"{0}\"")
  @ValueSource(
      strings = {
        "0123456789abcdef0123456789abcdef0123456",
        "0123456789abcdef0123456789abcdef012345678",
        "0123456789ABCDEF0123456789abcdef01234567",
        "0123456789abcdefg123456789abcdef01234567",
        ""
      })
  @DisplayName(
      "SHA fora de 40 caracteres hexadecimais minúsculos é rejeitado antes de qualquer chamada HTTP (CA25)")
  void shaForaDoFormatoERejeitadoSemChamadaHttp(String sha) {
    var resultado =
        ExecucaoDeScript.comAmbienteDeTeste()
            .comVariavel("RENDER_API_KEY", "chave-de-teste")
            .comVariavel("RENDER_SERVICE_ID", "srv-teste")
            .comVariavel("SERVICE_URL", servico.endereco())
            .rodarFuncao("executar_rollback", apiDoRender.endereco(), sha);

    assertThat(resultado.terminouComSucesso()).isFalse();
    assertThat(resultado.saida()).contains("40 caracteres hexadecimais minúsculos");
    assertThat(apiDoRender.pedidos()).isEmpty();
    assertThat(servico.pedidos()).isEmpty();
  }
}
