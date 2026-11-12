package br.com.fabianonr.podegastar.adaptador.entrada.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fabianonr.podegastar.configuracao.ConfiguracaoAplicacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(ConfiguracaoAplicacao.class)
@WebMvcTest(controllers = AvaliacaoController.class)
class IdDoItemNaRespostaHttpTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Item da sugestão traz o id interno, além do nome")
  void itemDaSugestaoTrazId() throws Exception {
    mockMvc
        .perform(
            get("/meses/mes-exemplo/avaliacao")
                .param("valor", "150.00")
                .param("categoria", "SERIA_LEGAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sugestoes[0].itens[0].id").value("item-6"))
        .andExpect(jsonPath("$.sugestoes[0].itens[0].nome").value("Bola de futebol"));
  }
}
