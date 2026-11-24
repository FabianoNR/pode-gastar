package br.com.fabianonr.podegastar.adaptador.entrada.web;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
class ItemProtegidoDeCorteHttpTest {

  private static final String ID_DO_CARTAO = "item-1";

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName(
      "No mês do servidor, o cartão protegido não aparece nas sugestões nem na lista completa (CA11)")
  void cartaoDoMesDoServidorEstaProtegidoDeCorte() throws Exception {
    mockMvc
        .perform(
            get("/meses/mes-exemplo/avaliacao")
                .param("valor", "1100.00")
                .param("categoria", "QUERO_FAZER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisao").value("CABE_COM_CORTES"))
        .andExpect(jsonPath("$.sugestoes", empty()))
        .andExpect(jsonPath("$.sugestoes[*].itens[*].id", not(hasItem(ID_DO_CARTAO))))
        .andExpect(jsonPath("$.listaCompleta[*].id", not(empty())))
        .andExpect(jsonPath("$.listaCompleta[*].id", not(hasItem(ID_DO_CARTAO))));
  }
}
