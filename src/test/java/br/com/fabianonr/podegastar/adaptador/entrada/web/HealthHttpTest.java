package br.com.fabianonr.podegastar.adaptador.entrada.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Contrato do /health com o commit no ar (O1 a O3). */
@WebMvcTest(controllers = HealthController.class)
class HealthHttpTest {

  private static final String VARIAVEL_DO_COMMIT = "RENDER_GIT_COMMIT";
  private static final String FONTE_DE_TESTE = "variaveis-de-teste";
  private static final String SHA = "0123456789abcdef0123456789abcdef01234567";

  @Autowired private MockMvc mockMvc;
  @Autowired private ConfigurableEnvironment ambiente;

  @AfterEach
  void removerVariavelDeTeste() {
    ambiente.getPropertySources().remove(FONTE_DE_TESTE);
  }

  @Test
  @DisplayName("GET /health com RENDER_GIT_COMMIT definida devolve UP e o valor da variável (CA13)")
  void healthDevolveOCommitDaVariavel() throws Exception {
    definirCommit(SHA);

    consultarHealth().andExpect(jsonPath("$.commit").value(SHA));
  }

  @Test
  @DisplayName("GET /health sem RENDER_GIT_COMMIT devolve UP e commit desconhecido (CA13)")
  @DisabledIfEnvironmentVariable(
      named = VARIAVEL_DO_COMMIT,
      matches = ".*",
      disabledReason = "RENDER_GIT_COMMIT existe no ambiente de quem roda os testes")
  void healthSemAVariavelDevolveCommitDesconhecido() throws Exception {
    consultarHealth().andExpect(jsonPath("$.commit").value("desconhecido"));
  }

  @Test
  @DisplayName("GET /health com RENDER_GIT_COMMIT vazia devolve commit desconhecido (CA21)")
  void healthComAVariavelVaziaDevolveCommitDesconhecido() throws Exception {
    definirCommit("");

    consultarHealth().andExpect(jsonPath("$.commit").value("desconhecido"));
  }

  @ParameterizedTest(name = "valor \"{0}\"")
  @ValueSource(
      strings = {
        "  0123456789abcdef0123456789abcdef01234567  ",
        "\t0123456789abcdef0123456789abcdef01234567\n",
        " \t0123456789abcdef0123456789abcdef01234567 \r\n"
      })
  @DisplayName("GET /health devolve o commit sem os espaços do início e do fim (CA30)")
  void healthDevolveOCommitAparado(String valorDaVariavel) throws Exception {
    definirCommit(valorDaVariavel);

    consultarHealth().andExpect(jsonPath("$.commit").value(SHA));
  }

  @ParameterizedTest(name = "valor \"{0}\"")
  @ValueSource(strings = {" ", "   ", "\t", " \n ", "\r\n"})
  @DisplayName(
      "GET /health com RENDER_GIT_COMMIT só com espaços devolve commit desconhecido (CA31)")
  void healthComAVariavelSoComEspacosDevolveCommitDesconhecido(String valorDaVariavel)
      throws Exception {
    definirCommit(valorDaVariavel);

    consultarHealth().andExpect(jsonPath("$.commit").value("desconhecido"));
  }

  @Test
  @DisplayName("GET /health reflete o valor atual de RENDER_GIT_COMMIT a cada consulta")
  void healthLeAVariavelACadaConsulta() throws Exception {
    definirCommit(SHA);
    consultarHealth().andExpect(jsonPath("$.commit").value(SHA));

    ambiente.getPropertySources().remove(FONTE_DE_TESTE);
    definirCommit("fedcba9876543210fedcba9876543210fedcba98");

    consultarHealth()
        .andExpect(jsonPath("$.commit").value("fedcba9876543210fedcba9876543210fedcba98"));
  }

  private void definirCommit(String valor) {
    ambiente
        .getPropertySources()
        .addFirst(new MapPropertySource(FONTE_DE_TESTE, Map.of(VARIAVEL_DO_COMMIT, valor)));
  }

  private ResultActions consultarHealth() throws Exception {
    return mockMvc
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("UP"));
  }
}
