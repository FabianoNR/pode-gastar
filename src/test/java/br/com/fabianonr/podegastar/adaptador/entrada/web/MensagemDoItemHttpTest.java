package br.com.fabianonr.podegastar.adaptador.entrada.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fabianonr.podegastar.adaptador.saida.memoria.MesEmMemoriaRepositorio;
import br.com.fabianonr.podegastar.aplicacao.AvaliarCompraService;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompra;
import br.com.fabianonr.podegastar.dominio.Categoria;
import br.com.fabianonr.podegastar.dominio.Item;
import br.com.fabianonr.podegastar.dominio.Mes;
import br.com.fabianonr.podegastar.dominio.StatusItem;
import java.math.BigDecimal;
import java.util.ArrayList;
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
class MensagemDoItemHttpTest {

  @TestConfiguration
  static class MesDeExemploComBolsa {

    @Bean
    AvaliarCompra avaliarCompra() {
      return new AvaliarCompraService(mesId -> Optional.of(mesDeExemploComBolsaEmDiscussao()));
    }

    private static Mes mesDeExemploComBolsaEmDiscussao() {
      Mes mesDeExemplo =
          new MesEmMemoriaRepositorio()
              .buscarPorId(MesEmMemoriaRepositorio.MES_EXEMPLO)
              .orElseThrow();
      List<Item> itens = new ArrayList<>(mesDeExemplo.itens());
      itens.add(
          new Item(
              "item-7",
              "Bolsa",
              Categoria.QUERO_FAZER,
              3,
              false,
              StatusItem.EM_DISCUSSAO,
              new BigDecimal("90"),
              null,
              "marido"));
      return new Mes(
          mesDeExemplo.id(),
          mesDeExemplo.renda(),
          mesDeExemplo.custoFixo(),
          mesDeExemplo.investimento(),
          itens);
    }
  }

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName(
      "Item em discussão na sugestão traz a mensagem pronta com o decisor responsável (CB1)")
  void itemEmDiscussaoTrazMensagemProntaDoServico() throws Exception {
    mockMvc
        .perform(
            get("/meses/{mesId}/avaliacao", MesEmMemoriaRepositorio.MES_EXEMPLO)
                .param("valor", "150.00")
                .param("categoria", "SERIA_LEGAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sugestoes[0].itens[0].id").value("item-7"))
        .andExpect(jsonPath("$.sugestoes[0].itens[0].decisorResponsavel").value("marido"))
        .andExpect(
            jsonPath("$.sugestoes[0].itens[0].mensagem").value("aguardando decisão de marido"))
        .andExpect(jsonPath("$.sugestoes[0].itens[1].id").value("item-6"))
        .andExpect(jsonPath("$.sugestoes[0].itens[1].mensagem").doesNotExist());
  }
}
