package br.com.fabianonr.podegastar.adaptador.entrada.web;

import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.test.web.servlet.ResultActions;

/** Formato das respostas do contrato da API (spec v6), sobre o mês de exemplo em memória. */
@Import(ConfiguracaoAplicacao.class)
@WebMvcTest(controllers = AvaliacaoController.class)
class ContratoAvaliacaoTest {

  @Autowired private MockMvc mockMvc;

  private ResultActions avaliar(String valor, String categoria) throws Exception {
    return mockMvc.perform(
        get("/meses/mes-exemplo/avaliacao").param("valor", valor).param("categoria", categoria));
  }

  @Test
  @DisplayName("200 APROVADO traz livre e saldoRestante, sem sugestões")
  void aprovadoTrazLivreESaldoRestanteSemSugestoes() throws Exception {
    avaliar("80.00", "QUERO_FAZER")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisao").value("APROVADO"))
        .andExpect(jsonPath("$.livre").value("100.00"))
        .andExpect(jsonPath("$.saldoRestante").value("20.00"))
        .andExpect(jsonPath("$.sugestoes").doesNotExist());
  }

  @Test
  @DisplayName(
      "200 CABE_COM_CORTES traz totalRestante, livre, falta, motivo, sugestoes e listaCompleta")
  void cabeComCortesTrazSugestoesEListaCompleta() throws Exception {
    avaliar("150.00", "SERIA_LEGAL")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisao").value("CABE_COM_CORTES"))
        .andExpect(jsonPath("$.totalRestante").value("1600.00"))
        .andExpect(jsonPath("$.livre").value("100.00"))
        .andExpect(jsonPath("$.falta").value("50.00"))
        .andExpect(jsonPath("$.motivo").isString())
        .andExpect(jsonPath("$.sugestoes", hasSize(1)))
        .andExpect(jsonPath("$.sugestoes[0].opcao").value("PADRAO"))
        .andExpect(jsonPath("$.sugestoes[0].itens[0].nome").value("Bola de futebol"))
        .andExpect(jsonPath("$.sugestoes[0].itens[0].status").value("PENDENTE"))
        .andExpect(jsonPath("$.sugestoes[0].cobre").value("80.00"))
        .andExpect(jsonPath("$.listaCompleta", hasSize(0)));
  }

  @Test
  @DisplayName("200 NAO_CABE traz totalRestante, falta e motivo, sem sugestões")
  void naoCabeTrazTotalRestanteFaltaEMotivoSemSugestoes() throws Exception {
    avaliar("1700.00", "QUERO_FAZER")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisao").value("NAO_CABE"))
        .andExpect(jsonPath("$.totalRestante").value("1600.00"))
        .andExpect(jsonPath("$.falta").value("100.00"))
        .andExpect(jsonPath("$.motivo").value("compra excede o total que ainda sobra no mês"))
        .andExpect(jsonPath("$.sugestoes").doesNotExist());
  }

  @Test
  @DisplayName("400 para categoria fora do conjunto, com a mensagem apontando o campo")
  void categoriaInvalidaRetorna400ApontandoOCampo() throws Exception {
    avaliar("80.00", "NAO_EXISTE")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campo").value("categoria"));
  }

  @Test
  @DisplayName("400 para valor com mais de duas casas decimais, apontando o campo")
  void valorComTresCasasRetorna400ApontandoOCampo() throws Exception {
    avaliar("80.123", "QUERO_FAZER")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campo").value("valor"));
  }

  @Test
  @DisplayName("400 para valor ausente ou não numérico, apontando o campo")
  void valorNaoNumericoRetorna400ApontandoOCampo() throws Exception {
    avaliar("abc", "QUERO_FAZER")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campo").value("valor"));
  }
}
