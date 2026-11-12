package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Critérios CB1 a CB4 da spec (v6), com a bolsa em discussão. */
class AvaliacaoComBolsaTest {

  private static Avaliacao avaliar(Mes mes, String valor, Categoria categoria) {
    return mes.avaliar(new Compra(valor(valor), categoria));
  }

  private static List<String> nomes(Sugestao sugestao) {
    return sugestao.itens().stream().map(Item::nome).toList();
  }

  @Test
  @DisplayName("150 SERIA_LEGAL, falta 140; sugere bolsa (marido) e bola, cobrem 170 (CB1)")
  void itemEmDiscussaoEntraNaSugestaoComODecisorResponsavel() {
    Mes mes = comExtras(bolsa());
    assertThat(mes.totalRestante()).isEqualByComparingTo("1600");
    assertThat(mes.livre()).isEqualByComparingTo("10");

    Avaliacao avaliacao = avaliar(mes, "150.00", Categoria.SERIA_LEGAL);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("140");
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(nomes(sugestao)).containsExactlyInAnyOrder(BOLSA, BOLA);
    assertThat(sugestao.cobre()).isEqualByComparingTo("170");
    Item bolsaSugerida =
        sugestao.itens().stream().filter(item -> item.nome().equals(BOLSA)).findFirst().get();
    assertThat(bolsaSugerida.status()).isEqualTo(StatusItem.EM_DISCUSSAO);
    assertThat(bolsaSugerida.decisorResponsavel()).isEqualTo("marido");
  }

  @Test
  @DisplayName("80 QUERO_FAZER, falta 70; a bolsa sozinha cobre (90) (CB2)")
  void itemEmDiscussaoSozinhoCobreAFalta() {
    Avaliacao avaliacao = avaliar(comExtras(bolsa()), "80.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("70");
    assertThat(nomes(avaliacao.sugestoes().get(0))).containsExactly(BOLSA);
    assertThat(avaliacao.sugestoes().get(0).cobre()).isEqualByComparingTo("90");
  }

  @Test
  @DisplayName("Bolsa DEVO_FAZER intensidade 2 em discussão continua elegível e vem primeiro (CB3)")
  void itemDevoFazerEmDiscussaoContinuaElegivelEVemPrimeiro() {
    Item bolsaDevo = emDiscussao(BOLSA, Categoria.DEVO_FAZER, 2, "90", "marido");

    Avaliacao avaliacao = avaliar(comExtras(bolsaDevo), "150.00", Categoria.SERIA_LEGAL);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(nomes(avaliacao.sugestoes().get(0)).get(0)).isEqualTo(BOLSA);
  }

  @Test
  @DisplayName("Entre dois EM_DISCUSSAO, a categoria pesa antes da intensidade (CB4)")
  void entreItensEmDiscussaoCategoriaPesaAntesDaIntensidade() {
    Item quero5 = emDiscussao("Item QUERO", Categoria.QUERO_FAZER, 5, "50", "marido");
    Item devo2 = emDiscussao("Item DEVO", Categoria.DEVO_FAZER, 2, "50", "marido");

    // Livre = 0; falta 80 só se fecha com os dois itens (50 + 50).
    Avaliacao avaliacao = avaliar(comExtras(devo2, quero5), "80.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("80");
    assertThat(nomes(avaliacao.sugestoes().get(0))).containsExactly("Item QUERO", "Item DEVO");
    assertThat(avaliacao.sugestoes().get(0).cobre()).isEqualByComparingTo("100");
  }
}
