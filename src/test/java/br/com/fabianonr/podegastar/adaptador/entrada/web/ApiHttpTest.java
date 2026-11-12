package br.com.fabianonr.podegastar.adaptador.entrada.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fabianonr.podegastar.configuracao.ConfiguracaoAplicacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Contrato HTTP da spec (v6): CA12, 404 para mesId inexistente e CA13. */
@Import(ConfiguracaoAplicacao.class)
@WebMvcTest(controllers = {AvaliacaoController.class, HealthController.class})
class ApiHttpTest {

  private static final String MES_ID = "mes-exemplo";

  @Autowired private MockMvc mockMvc;

  @ParameterizedTest(name = "valor={0} retorna 400 (CA12)")
  @ValueSource(strings = {"0", "0.00", "-10.00"})
  @DisplayName("Valor zero ou negativo retorna 400 (CA12)")
  void valorZeroOuNegativoRetorna400(String valor) throws Exception {
    mockMvc
        .perform(
            get("/meses/{mesId}/avaliacao", MES_ID)
                .param("valor", valor)
                .param("categoria", "QUERO_FAZER"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("404: mesId inexistente retorna 404")
  void mesIdInexistenteRetorna404() throws Exception {
    mockMvc
        .perform(
            get("/meses/{mesId}/avaliacao", "nao-existe")
                .param("valor", "80.00")
                .param("categoria", "QUERO_FAZER"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /health retorna 200 com {\"status\": \"UP\"} (CA13)")
  void healthRetornaStatusUp() throws Exception {
    mockMvc
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("UP"));
  }
}
