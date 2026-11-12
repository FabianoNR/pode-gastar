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
    return avaliacao.sugestoes().stream()
        .flatMap(sugestao -> sugestao.itens().stream())
        .map(Item::nome)
        .toList();
  }

  @Test
  @DisplayName("80 QUERO_FAZER é APROVADO, saldo restante 20, sem sugestão (CA1)")
  void compraQueCabeNoLivreEhAprovadaSemSugestao() {
    Avaliacao avaliacao = avaliar(base(), "80.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.APROVADO);
    assertThat(avaliacao.livre()).isEqualByComparingTo("100");
    assertThat(avaliacao.saldoRestante()).isEqualByComparingTo("20");
    assertThat(avaliacao.sugestoes()).isEmpty();
  }

  @Test
  @DisplayName("300 QUERO_FAZER cabe com cortes, falta 200, sugere jantar e bola (200) (CA2)")
  void compraQueroFazerAcimaDoLivreSugereCortarSeriaLegal() {
    Avaliacao avaliacao = avaliar(base(), "300.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("200");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    assertThat(nomes(avaliacao.sugestoes().get(0))).containsExactlyInAnyOrder(JANTAR, BOLA);
    assertThat(avaliacao.sugestoes().get(0).cobre()).isEqualByComparingTo("200");
  }

  @Test
  @DisplayName("150 SERIA_LEGAL cabe com cortes, falta 50, sugere a bola de futebol (CA3)")
  void compraSeriaLegalAcimaDoLivreSugereOItemSeriaLegalDeMenorIntensidade() {
    Avaliacao avaliacao = avaliar(base(), "150.00", Categoria.SERIA_LEGAL);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("50");
    assertThat(nomes(avaliacao.sugestoes().get(0))).containsExactly(BOLA);
    assertThat(avaliacao.sugestoes().get(0).cobre()).isEqualByComparingTo("80");
  }

  @Test
  @DisplayName("250 DEVO_FAZER cabe com cortes, falta 150, sugere jantar e bola (CA4)")
  void compraDevoFazerAcimaDoLivreSugereCortarSeriaLegalPrimeiro() {
    Avaliacao avaliacao = avaliar(base(), "250.00", Categoria.DEVO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("150");
    assertThat(nomes(avaliacao.sugestoes().get(0))).containsExactlyInAnyOrder(JANTAR, BOLA);
  }

  @Test
  @DisplayName("800 DEVO_FAZER, falta 700; opção 1 fecha exato com SERIA_LEGAL e QUERO_FAZER (CA5)")
  void compraDevoFazerGrandeTemOpcaoQueFechaExatoComSeriaLegalEQueroFazer() {
    Avaliacao avaliacao = avaliar(base(), "800.00", Categoria.DEVO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("700");
    assertThat(avaliacao.sugestoes())
        .anySatisfy(sugestao -> {
          assertThat(nomes(sugestao)).containsExactlyInAnyOrder(JANTAR, BOLA, VIAGEM, ITEM_CASA);
          assertThat(sugestao.cobre()).isEqualByComparingTo("700");
        });
  }

  @Test
  @DisplayName("1700 QUERO_FAZER é NAO_CABE, falta 100, sem sugestões (CA6)")
  void compraAcimaDoTotalRestanteNaoCabe() {
    Avaliacao avaliacao = avaliar(base(), "1700.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.NAO_CABE);
    assertThat(avaliacao.totalRestante()).isEqualByComparingTo("1600");
    assertThat(avaliacao.falta()).isEqualByComparingTo("100");
    assertThat(avaliacao.motivo()).isEqualTo(Motivo.COMPRA_EXCEDE_TOTAL_RESTANTE);
    assertThat(avaliacao.sugestoes()).isEmpty();
  }

  @Test
  @DisplayName(
      "Viagem PARCIAL (realizado 150): total restante 1450, livre 100; 100 QUERO aprova com saldo 0 (CA7)")
  void itemParcialReduzTotalRestanteEMantemComprometidoOEstimado() {
    Mes mes = comViagem(viagem(StatusItem.PARCIAL, "150"));

    assertThat(mes.totalRestante()).isEqualByComparingTo("1450");
    assertThat(mes.livre()).isEqualByComparingTo("100");

    Avaliacao avaliacao = avaliar(mes, "100.00", Categoria.QUERO_FAZER);
    assertThat(avaliacao.decisao()).isEqualTo(Decisao.APROVADO);
    assertThat(avaliacao.saldoRestante()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName(
      "Viagem REALIZADO (250): total restante 1350, livre 150; 150 QUERO aprova com saldo 0 (CA8)")
  void itemRealizadoComprometeSoOValorRealizado() {
    Mes mes = comViagem(viagem(StatusItem.REALIZADO, "250"));

    assertThat(mes.totalRestante()).isEqualByComparingTo("1350");
    assertThat(mes.livre()).isEqualByComparingTo("150");

    Avaliacao avaliacao = avaliar(mes, "150.00", Categoria.QUERO_FAZER);
    assertThat(avaliacao.decisao()).isEqualTo(Decisao.APROVADO);
    assertThat(avaliacao.saldoRestante()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName(
      "Viagem REALIZADO (250) e 200 QUERO: falta 50, sugere a bola; a viagem realizada nunca aparece (CA9)")
  void itemRealizadoNuncaApareceNaSugestaoNemNaListaCompleta() {
    Avaliacao avaliacao =
        avaliar(comViagem(viagem(StatusItem.REALIZADO, "250")), "200.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("50");
    assertThat(nomes(avaliacao.sugestoes().get(0))).containsExactly(BOLA);
    assertThat(nomesSugeridos(avaliacao)).doesNotContain(VIAGEM);
    assertThat(avaliacao.listaCompleta().stream().map(Item::nome)).doesNotContain(VIAGEM);
  }

  /**
   * CA10: a spec não define em que posição de R6 o item PARCIAL entra na ordem das sugestões. Aqui
   * só se verifica o que é derivável: a decisão e a falta, com a viagem PARCIAL contando 300 como
   * comprometido (livre 100) e 150 como já saído (total restante 1450). A pergunta sobre quando a
   * viagem aparece como sugestão está aberta na PR.
   */
  @Test
  @DisplayName(
      "Viagem PARCIAL (150) e 200 QUERO com livre insuficiente: cabe com cortes, falta 100 (CA10)")
  void compraAcimaDoLivreComItemParcialCabeComCortes() {
    Mes mes = comViagem(viagem(StatusItem.PARCIAL, "150"));
    Avaliacao avaliacao = avaliar(mes, "200.00", Categoria.QUERO_FAZER);

    assertThat(mes.totalRestante()).isEqualByComparingTo("1450");
    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("100");
  }

  @Test
  @DisplayName("O cartão de crédito nunca aparece como sugestão de corte (CA11)")
  void cartaoDeCreditoNuncaApareceComoSugestaoDeCorte() {
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
