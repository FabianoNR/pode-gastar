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
  @DisplayName("CB1: 150 SERIA_LEGAL, falta 140; sugere bolsa (marido) e bola, cobrem 170")
  void cb1() {
    Mes mes = comExtras(bolsa());
    assertThat(mes.totalRestante()).isEqualByComparingTo("1600");
    assertThat(mes.livre()).isEqualByComparingTo("10");

    Avaliacao a = avaliar(mes, "150.00", Categoria.SERIA_LEGAL);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.falta()).isEqualByComparingTo("140");
    Sugestao s = a.sugestoes().get(0);
    assertThat(nomes(s)).containsExactlyInAnyOrder(BOLSA, BOLA);
    assertThat(s.cobre()).isEqualByComparingTo("170");
    Item bolsaSugerida = s.itens().stream().filter(i -> i.nome().equals(BOLSA)).findFirst().get();
    assertThat(bolsaSugerida.status()).isEqualTo(StatusItem.EM_DISCUSSAO);
    assertThat(bolsaSugerida.decisorResponsavel()).isEqualTo("marido");
  }

  @Test
  @DisplayName("CB2: 80 QUERO_FAZER, falta 70; a bolsa sozinha cobre (90)")
  void cb2() {
    Avaliacao a = avaliar(comExtras(bolsa()), "80.00", Categoria.QUERO_FAZER);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.falta()).isEqualByComparingTo("70");
    assertThat(nomes(a.sugestoes().get(0))).containsExactly(BOLSA);
    assertThat(a.sugestoes().get(0).cobre()).isEqualByComparingTo("90");
  }

  @Test
  @DisplayName("CB3: bolsa DEVO_FAZER intensidade 2 em discussão continua elegível e vem primeiro")
  void cb3() {
    Item bolsaDevo = emDiscussao(BOLSA, Categoria.DEVO_FAZER, 2, "90", "marido");

    Avaliacao a = avaliar(comExtras(bolsaDevo), "150.00", Categoria.SERIA_LEGAL);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(nomes(a.sugestoes().get(0)).get(0)).isEqualTo(BOLSA);
  }

  @Test
  @DisplayName("CB4: entre dois EM_DISCUSSAO, a categoria pesa antes da intensidade")
  void cb4() {
    Item quero5 = emDiscussao("Item QUERO", Categoria.QUERO_FAZER, 5, "50", "marido");
    Item devo2 = emDiscussao("Item DEVO", Categoria.DEVO_FAZER, 2, "50", "marido");

    // Livre = 0; falta 80 só se fecha com os dois itens (50 + 50).
    Avaliacao a = avaliar(comExtras(devo2, quero5), "80.00", Categoria.QUERO_FAZER);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.falta()).isEqualByComparingTo("80");
    assertThat(nomes(a.sugestoes().get(0))).containsExactly("Item QUERO", "Item DEVO");
    assertThat(a.sugestoes().get(0).cobre()).isEqualByComparingTo("100");
  }
}
