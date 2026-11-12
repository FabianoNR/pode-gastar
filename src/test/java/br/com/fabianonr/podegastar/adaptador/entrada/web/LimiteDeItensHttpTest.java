package br.com.fabianonr.podegastar.adaptador.entrada.web;

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
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AvaliacaoController.class)
class LimiteDeItensHttpTest {

  private static final String MES_COM_ITENS_DEMAIS = "mes-com-itens-demais";

  @TestConfiguration
  static class MesComVinteEUmItens {

    @Bean
    AvaliarCompra avaliarCompra() {
      return new AvaliarCompraService(mesId -> Optional.of(mesComVinteEUmItensSeriaLegal()));
    }

    private static Mes mesComVinteEUmItensSeriaLegal() {
      List<Item> itens =
          IntStream.rangeClosed(1, 21)
              .mapToObj(numero ->
                  new Item(
                      "item-" + numero,
                      "Item " + numero,
                      Categoria.SERIA_LEGAL,
                      1,
                      true,
                      StatusItem.PENDENTE,
                      new BigDecimal("10"),
                      null,
                      null))
              .toList();
      // Orçamento 1000, comprometido 210: livre 790, então uma compra de 800 exige cortes.
      return new Mes(
          MES_COM_ITENS_DEMAIS, new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO, itens);
    }
  }

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Grupo com mais de 20 itens responde 422 com mensagem clara em português")
  void respondeErroDeContratoQuandoGrupoTemItensDemais() throws Exception {
    mockMvc
        .perform(
            get("/meses/{mesId}/avaliacao", MES_COM_ITENS_DEMAIS)
                .param("valor", "800.00")
                .param("categoria", "SERIA_LEGAL"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "itens demais para calcular sugestões de corte neste mês (máximo 20 por grupo)"));
  }
}
