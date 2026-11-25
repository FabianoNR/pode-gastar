package br.com.fabianonr.podegastar.adaptador.entrada.web;

import static br.com.fabianonr.podegastar.dominio.ItemDeTeste.umItem;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fabianonr.podegastar.aplicacao.AvaliarComprasService;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompras;
import br.com.fabianonr.podegastar.dominio.Categoria;
import br.com.fabianonr.podegastar.dominio.Item;
import br.com.fabianonr.podegastar.dominio.Mes;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Consulta de vários itens em meses diferentes do de exemplo: nome repetido e limite de itens. */
@WebMvcTest(controllers = AvaliacoesController.class)
class ConsultaDeVariosItensEmOutrosMesesHttpTest {

  private static final String MES_COM_NOME_REPETIDO = "mes-com-nome-repetido";
  private static final String MES_COM_21_EM_DISCUSSAO = "mes-com-21-em-discussao";

  @TestConfiguration
  static class MesesDeTeste {

    @Bean
    AvaliarCompras avaliarCompras() {
      return new AvaliarComprasService(mesId ->
          Optional.of(
              MES_COM_NOME_REPETIDO.equals(mesId)
                  ? mesComNomeRepetido()
                  : mesComVinteEUmItensEmDiscussao()));
    }

    private static Mes mesComNomeRepetido() {
      return new Mes(
          MES_COM_NOME_REPETIDO,
          new BigDecimal("1000"),
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          List.of(
              umItem("item-5", "Jantar fora", Categoria.SERIA_LEGAL, 2, "100").criar(),
              umItem("item-9", "Jantar fora", Categoria.SERIA_LEGAL, 2, "100").criar()));
    }

    /** Orçamento variável 1000, 21 itens em discussão de 10: livre 790, total restante 1000. */
    private static Mes mesComVinteEUmItensEmDiscussao() {
      List<Item> itens =
          IntStream.rangeClosed(1, 21)
              .mapToObj(numero ->
                  umItem("item-" + numero, "Pedido " + numero, Categoria.QUERO_FAZER, 1, "10")
                      .emDiscussaoAguardando("marido")
                      .criar())
              .toList();
      return new Mes(
          MES_COM_21_EM_DISCUSSAO, new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO, itens);
    }
  }

  @Autowired private MockMvc mockMvc;

  private ResultActions consultar(String mesId, String corpo) throws Exception {
    return mockMvc.perform(
        post("/meses/{mesId}/avaliacoes", mesId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  @Test
  @DisplayName("O aviso de nome repetido do mês vem uma vez no topo e em nenhuma avaliação (CA77)")
  void avisoDeNomeRepetidoVemUmaVezNoTopo() throws Exception {
    consultar(
            MES_COM_NOME_REPETIDO,
            "{\"itens\": ["
                + "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\"},"
                + " {\"nome\": \"Caderno\", \"valor\": 30.00, \"categoria\": \"QUERO_FAZER\"}]}")
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.avisos")
                .value(
                    contains(
                        "Nome repetido neste mês: \"Jantar fora\" (ids: item-5, item-9)."
                            + " São itens diferentes.")))
        .andExpect(jsonPath("$.avaliacoes", hasSize(2)))
        .andExpect(jsonPath("$.avaliacoes[0].avisos", hasSize(0)))
        .andExpect(jsonPath("$.avaliacoes[1].avisos", hasSize(0)));
  }

  @Test
  @DisplayName("Um item que cai no limite de 20 itens por grupo derruba a lista com 422 (CA82)")
  void itemNoLimiteDeItensDerrubaAListaCom422() throws Exception {
    consultar(
            MES_COM_21_EM_DISCUSSAO,
            "{\"itens\": ["
                + "{\"nome\": \"A\", \"valor\": 100.00, \"categoria\": \"QUERO_FAZER\"},"
                + " {\"nome\": \"B\", \"valor\": 900.00, \"categoria\": \"QUERO_FAZER\"}]}")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "itens demais para calcular sugestões de corte neste mês (máximo 20 por grupo)"))
        .andExpect(jsonPath("$.avaliacoes").doesNotExist());
  }

  @Test
  @DisplayName("No mesmo mês, um item que cabe no livre é aprovado sem checar o limite (CA82)")
  void itemQueCabeNoLivreNaoChecaOLimite() throws Exception {
    consultar(
            MES_COM_21_EM_DISCUSSAO,
            "{\"itens\": [{\"nome\": \"A\", \"valor\": 100.00, \"categoria\": \"QUERO_FAZER\"}]}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avaliacoes[0].decisao").value("APROVADO"));
  }
}
