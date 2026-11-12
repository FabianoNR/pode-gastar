package br.com.fabianonr.podegastar.adaptador.entrada.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fabianonr.podegastar.aplicacao.AvaliarCompraService;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompra;
import br.com.fabianonr.podegastar.dominio.Categoria;
import br.com.fabianonr.podegastar.dominio.Item;
import br.com.fabianonr.podegastar.dominio.Mes;
import br.com.fabianonr.podegastar.dominio.StatusItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AvaliacaoController.class)
class AvisosHttpTest {

  private static final String MES_COM_NOMES_REPETIDOS = "mes-com-nomes-repetidos";
  private static final String MES_SEM_NOMES_REPETIDOS = "mes-sem-nomes-repetidos";

  @TestConfiguration
  static class MesesDeTeste {

    @Bean
    AvaliarCompra avaliarCompra() {
      return new AvaliarCompraService(mesId ->
          Optional.of(
              mesId.equals(MES_COM_NOMES_REPETIDOS)
                  ? mes(
                      mesId,
                      item("item-5", "Jantar fora"),
                      item("item-9", " jantar FORA "),
                      item("item-6", "Bola de futebol"))
                  : mes(mesId, item("item-5", "Jantar fora"), item("item-6", "Bola de futebol"))));
    }

    private static Item item(String id, String nome) {
      return new Item(
          id,
          nome,
          Categoria.SERIA_LEGAL,
          2,
          true,
          StatusItem.PENDENTE,
          new BigDecimal("100"),
          null,
          null);
    }

    private static Mes mes(String mesId, Item... itens) {
      return new Mes(
          mesId, new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO, List.of(itens));
    }
  }

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Mês com nomes repetidos traz um aviso por nome, citando os nomes e os ids")
  void trazAvisoPorNomeRepetido() throws Exception {
    mockMvc
        .perform(
            get("/meses/{mesId}/avaliacao", MES_COM_NOMES_REPETIDOS)
                .param("valor", "80.00")
                .param("categoria", "SERIA_LEGAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisao").value("APROVADO"))
        .andExpect(jsonPath("$.avisos", hasSize(1)))
        .andExpect(
            jsonPath("$.avisos[0]")
                .value(
                    "Nome repetido neste mês: \"Jantar fora\", \" jantar FORA \" (ids: item-5, item-9). São itens diferentes."));
  }

  @Test
  @DisplayName("Sem nomes repetidos, a resposta traz avisos como lista vazia")
  void trazAvisosVazioQuandoNaoHaNomeRepetido() throws Exception {
    mockMvc
        .perform(
            get("/meses/{mesId}/avaliacao", MES_SEM_NOMES_REPETIDOS)
                .param("valor", "80.00")
                .param("categoria", "SERIA_LEGAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avisos", hasSize(0)));
  }
}
