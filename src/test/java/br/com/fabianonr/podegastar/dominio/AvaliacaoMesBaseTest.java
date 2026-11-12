package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Critérios CA1 a CA11 da spec (v6), sobre o mês de exemplo sem a bolsa. */
class AvaliacaoMesBaseTest {

  private static Avaliacao avaliar(Mes mes, String valor, Categoria categoria) {
    return mes.avaliar(new Compra(valor(valor), categoria));
  }

  private static List<String> nomes(Sugestao sugestao) {
    return sugestao.itens().stream().map(Item::nome).toList();
  }

  private static List<String> nomesSugeridos(Avaliacao avaliacao) {
    return avaliacao.sugestoes().stream().flatMap(s -> s.itens().stream()).map(Item::nome).toList();
  }

  @Test
  @DisplayName("CA1: 80 QUERO_FAZER é APROVADO, saldo restante 20, sem sugestão")
  void ca1() {
    Avaliacao a = avaliar(base(), "80.00", Categoria.QUERO_FAZER);

    assertThat(a.decisao()).isEqualTo(Decisao.APROVADO);
    assertThat(a.livre()).isEqualByComparingTo("100");
    assertThat(a.saldoRestante()).isEqualByComparingTo("20");
    assertThat(a.sugestoes()).isEmpty();
  }

  @Test
  @DisplayName("CA2: 300 QUERO_FAZER cabe com cortes, falta 200, sugere jantar e bola (200)")
  void ca2() {
    Avaliacao a = avaliar(base(), "300.00", Categoria.QUERO_FAZER);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.falta()).isEqualByComparingTo("200");
    assertThat(a.sugestoes()).hasSize(1);
    assertThat(nomes(a.sugestoes().get(0))).containsExactlyInAnyOrder(JANTAR, BOLA);
    assertThat(a.sugestoes().get(0).cobre()).isEqualByComparingTo("200");
  }

  @Test
  @DisplayName("CA3: 150 SERIA_LEGAL cabe com cortes, falta 50, sugere a bola de futebol")
  void ca3() {
    Avaliacao a = avaliar(base(), "150.00", Categoria.SERIA_LEGAL);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.falta()).isEqualByComparingTo("50");
    assertThat(nomes(a.sugestoes().get(0))).containsExactly(BOLA);
    assertThat(a.sugestoes().get(0).cobre()).isEqualByComparingTo("80");
  }

  @Test
  @DisplayName("CA4: 250 DEVO_FAZER cabe com cortes, falta 150, sugere jantar e bola")
  void ca4() {
    Avaliacao a = avaliar(base(), "250.00", Categoria.DEVO_FAZER);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.falta()).isEqualByComparingTo("150");
    assertThat(nomes(a.sugestoes().get(0))).containsExactlyInAnyOrder(JANTAR, BOLA);
  }

  @Test
  @DisplayName("CA5: 800 DEVO_FAZER, falta 700; opção 1 fecha exato com SERIA_LEGAL e QUERO_FAZER")
  void ca5() {
    Avaliacao a = avaliar(base(), "800.00", Categoria.DEVO_FAZER);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.falta()).isEqualByComparingTo("700");
    assertThat(a.sugestoes())
        .anySatisfy(s -> {
          assertThat(nomes(s)).containsExactlyInAnyOrder(JANTAR, BOLA, VIAGEM, ITEM_CASA);
          assertThat(s.cobre()).isEqualByComparingTo("700");
        });
  }

  @Test
  @DisplayName("CA6: 1700 QUERO_FAZER é NAO_CABE, falta 100, sem sugestões")
  void ca6() {
    Avaliacao a = avaliar(base(), "1700.00", Categoria.QUERO_FAZER);

    assertThat(a.decisao()).isEqualTo(Decisao.NAO_CABE);
    assertThat(a.totalRestante()).isEqualByComparingTo("1600");
    assertThat(a.falta()).isEqualByComparingTo("100");
    assertThat(a.motivo()).isEqualTo(Motivo.COMPRA_EXCEDE_TOTAL_RESTANTE);
    assertThat(a.sugestoes()).isEmpty();
  }

  @Test
  @DisplayName(
      "CA7: viagem PARCIAL (realizado 150): total restante 1450, livre 100; 100 QUERO aprova com saldo 0")
  void ca7() {
    Mes mes = comViagem(viagem(StatusItem.PARCIAL, "150"));

    assertThat(mes.totalRestante()).isEqualByComparingTo("1450");
    assertThat(mes.livre()).isEqualByComparingTo("100");

    Avaliacao a = avaliar(mes, "100.00", Categoria.QUERO_FAZER);
    assertThat(a.decisao()).isEqualTo(Decisao.APROVADO);
    assertThat(a.saldoRestante()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName(
      "CA8: viagem REALIZADO (250): total restante 1350, livre 150; 150 QUERO aprova com saldo 0")
  void ca8() {
    Mes mes = comViagem(viagem(StatusItem.REALIZADO, "250"));

    assertThat(mes.totalRestante()).isEqualByComparingTo("1350");
    assertThat(mes.livre()).isEqualByComparingTo("150");

    Avaliacao a = avaliar(mes, "150.00", Categoria.QUERO_FAZER);
    assertThat(a.decisao()).isEqualTo(Decisao.APROVADO);
    assertThat(a.saldoRestante()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName(
      "CA9: caso CA8 com 200 QUERO: falta 50, sugere a bola; a viagem realizada nunca aparece")
  void ca9() {
    Avaliacao a =
        avaliar(comViagem(viagem(StatusItem.REALIZADO, "250")), "200.00", Categoria.QUERO_FAZER);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.falta()).isEqualByComparingTo("50");
    assertThat(nomes(a.sugestoes().get(0))).containsExactly(BOLA);
    assertThat(nomesSugeridos(a)).doesNotContain(VIAGEM);
    assertThat(a.listaCompleta().stream().map(Item::nome)).doesNotContain(VIAGEM);
  }

  /**
   * CA10: a spec não define em que posição de R6 o item PARCIAL entra na ordem das sugestões. Aqui
   * só se verifica o que é derivável: a decisão e a falta, com a viagem PARCIAL contando 300 como
   * comprometido (livre 100) e 150 como já saído (total restante 1450). A pergunta sobre quando a
   * viagem aparece como sugestão está aberta na PR.
   */
  @Test
  @DisplayName(
      "CA10: viagem PARCIAL (150) e 200 QUERO com livre insuficiente: cabe com cortes, falta 100")
  void ca10() {
    Mes mes = comViagem(viagem(StatusItem.PARCIAL, "150"));
    Avaliacao a = avaliar(mes, "200.00", Categoria.QUERO_FAZER);

    assertThat(mes.totalRestante()).isEqualByComparingTo("1450");
    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.falta()).isEqualByComparingTo("100");
  }

  @Test
  @DisplayName("CA11: o cartão de crédito nunca aparece como sugestão de corte")
  void ca11() {
    Avaliacao comOpcoes = avaliar(base(), "800.00", Categoria.DEVO_FAZER);
    // 1600 não é coberto por nenhuma combinação elegível: sai a lista completa.
    Avaliacao semCobertura = avaliar(base(), "1600.00", Categoria.DEVO_FAZER);

    assertThat(nomesSugeridos(comOpcoes)).doesNotContain(CARTAO);
    assertThat(nomesSugeridos(semCobertura)).doesNotContain(CARTAO);
    assertThat(semCobertura.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(semCobertura.listaCompleta()).isNotEmpty();
    assertThat(semCobertura.listaCompleta().stream().map(Item::nome)).doesNotContain(CARTAO);
  }
}
