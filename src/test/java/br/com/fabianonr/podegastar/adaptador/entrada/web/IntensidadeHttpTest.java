package br.com.fabianonr.podegastar.adaptador.entrada.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Parâmetro intensidade e campo itensMaisDesejadosQueACompra, sobre o mês de exemplo. */
@Import(ConfiguracaoAplicacao.class)
@WebMvcTest(controllers = AvaliacaoController.class)
class IntensidadeHttpTest {

  @Autowired private MockMvc mockMvc;

  private ResultActions avaliar(String valor, String intensidade) throws Exception {
    MockHttpServletRequestBuilder consulta =
        get("/meses/mes-exemplo/avaliacao").param("valor", valor).param("categoria", "QUERO_FAZER");
    if (intensidade != null) {
      consulta = consulta.param("intensidade", intensidade);
    }
    return mockMvc.perform(consulta);
  }

  @Test
  @DisplayName("Intensidade 5: o campo novo vem como lista vazia e avisos sem mensagem (CA65)")
  void intensidadeCincoTrazOCampoVazio() throws Exception {
    avaliar("300.00", "5")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisao").value("CABE_COM_CORTES"))
        .andExpect(jsonPath("$.falta").value("200.00"))
        .andExpect(jsonPath("$.sugestoes[0].itens", hasSize(2)))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra", hasSize(0)))
        .andExpect(jsonPath("$.avisos", hasSize(0)));
  }

  @Test
  @DisplayName(
      "Intensidade 1: o campo novo lista jantar fora e bola, e avisos traz a mensagem (CA66)")
  void intensidadeUmListaOsItensEAvisa() throws Exception {
    avaliar("300.00", "1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisao").value("CABE_COM_CORTES"))
        .andExpect(jsonPath("$.sugestoes[0].itens", hasSize(2)))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra", hasSize(2)))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra[0].id").value("item-5"))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra[0].nome").value("Jantar fora no mês"))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra[0].intensidade").value(2))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra[1].nome").value("Bola de futebol"))
        .andExpect(jsonPath("$.avisos", hasSize(1)))
        .andExpect(
            jsonPath("$.avisos[0]")
                .value(
                    "A compra (intensidade 1) é menos desejada que os itens a cortar:"
                        + " \"Jantar fora no mês\", \"Bola de futebol\"."));
  }

  @Test
  @DisplayName("Sem intensidade a resposta é a do CA66, e o aviso diz que valeu 1 (CA67)")
  void semIntensidadeAvisaQueFoiConsideradaUm() throws Exception {
    avaliar("300.00", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra", hasSize(2)))
        .andExpect(jsonPath("$.avisos", hasSize(1)))
        .andExpect(
            jsonPath("$.avisos[0]")
                .value(
                    "A intensidade da compra não foi informada e foi considerada 1."
                        + " A compra é menos desejada que os itens a cortar:"
                        + " \"Jantar fora no mês\", \"Bola de futebol\"."));
  }

  @Test
  @DisplayName("Intensidade 3: o campo novo traz só a viagem, de intensidade 4 (CA68)")
  void intensidadeTresTrazSoAViagem() throws Exception {
    avaliar("600.00", "3")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sugestoes[0].itens", hasSize(3)))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra", hasSize(1)))
        .andExpect(
            jsonPath("$.itensMaisDesejadosQueACompra[0].nome").value("Viagem de fim de semana"))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra[0].intensidade").value(4));
  }

  @Test
  @DisplayName("Intensidade 4: a viagem tem intensidade igual e o campo novo vem vazio (CA69)")
  void intensidadeQuatroTrazOCampoVazio() throws Exception {
    avaliar("600.00", "4")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sugestoes[0].itens", hasSize(3)))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra", hasSize(0)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "6", "2.5", "abc"})
  @DisplayName("Intensidade fora de 1 a 5 ou não inteira dá 400 apontando intensidade (CA70)")
  void intensidadeInvalidaDa400ApontandoOCampo(String intensidade) throws Exception {
    avaliar("300.00", intensidade)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campo").value("intensidade"));
  }

  @Test
  @DisplayName("APROVADO com intensidade informada não traz o campo novo (CA71)")
  void aprovadoNaoTrazOCampoNovo() throws Exception {
    avaliar("80.00", "1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisao").value("APROVADO"))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra").doesNotExist())
        .andExpect(jsonPath("$.avisos", hasSize(0)));
  }

  @Test
  @DisplayName("NAO_CABE com intensidade informada não traz o campo novo (CA71)")
  void naoCabeNaoTrazOCampoNovo() throws Exception {
    avaliar("1700.00", "1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisao").value("NAO_CABE"))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra").doesNotExist())
        .andExpect(jsonPath("$.avisos", hasSize(0)));
  }

  @Test
  @DisplayName(
      "Lista completa: o campo novo traz viagem e manutenção, e sugestoes segue vazia (CA72)")
  void listaCompletaTrazViagemEManutencao() throws Exception {
    avaliar("1100.00", "3")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sugestoes", hasSize(0)))
        .andExpect(jsonPath("$.listaCompleta", hasSize(5)))
        .andExpect(
            jsonPath("$.itensMaisDesejadosQueACompra[*].nome")
                .value(contains("Viagem de fim de semana", "Manutenção do carro")))
        .andExpect(jsonPath("$.itensMaisDesejadosQueACompra[*].intensidade").value(contains(4, 5)));
  }
}
