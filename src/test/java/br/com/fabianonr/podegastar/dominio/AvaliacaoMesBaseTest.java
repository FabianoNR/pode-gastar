package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Critérios CA1 a CA11, CA14 a CA16, CA19 e CA20 da spec (v8), no mês de exemplo sem a bolsa. */
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
    assertThat(nomes(avaliacao.sugestoes().get(0))).containsExactly(JANTAR, BOLA);
    assertThat(avaliacao.sugestoes().get(0).cobre()).isEqualByComparingTo("200");
  }

  @Test
  @DisplayName("150 SERIA_LEGAL cabe com cortes, falta 50, sugere a bola de futebol (CA3)")
  void compraSeriaLegalAcimaDoLivreSugereOItemSeriaLegalDeMenorIntensidade() {
    Avaliacao avaliacao = avaliar(base(), "150.00", Categoria.SERIA_LEGAL);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("50");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    assertThat(nomes(avaliacao.sugestoes().get(0))).containsExactly(BOLA);
    assertThat(avaliacao.sugestoes().get(0).cobre()).isEqualByComparingTo("80");
  }

  @Test
  @DisplayName("250 DEVO_FAZER cabe com cortes, falta 150, sugere jantar e bola (CA4)")
  void compraDevoFazerAcimaDoLivreSugereCortarSeriaLegalPrimeiro() {
    Avaliacao avaliacao = avaliar(base(), "250.00", Categoria.DEVO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("150");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    assertThat(nomes(avaliacao.sugestoes().get(0))).containsExactly(JANTAR, BOLA);
    assertThat(avaliacao.sugestoes().get(0).cobre()).isEqualByComparingTo("200");
  }

  @Test
  @DisplayName("800 DEVO_FAZER, falta 700; a regra de corte fecha exato sem chegar às opções (CA5)")
  void compraDevoFazerQueARegraDeCorteCobreNaoChegaAsOpcoes() {
    Avaliacao avaliacao = avaliar(base(), "800.00", Categoria.DEVO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("700");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(sugestao.opcao()).isEqualTo(Opcao.PADRAO);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, BOLA, ITEM_CASA, VIAGEM);
    assertThat(sugestao.cobre()).isEqualByComparingTo("700");
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
    assertThat(avaliacao.sugestoes()).hasSize(1);
    assertThat(nomes(avaliacao.sugestoes().get(0))).containsExactly(BOLA);
    assertThat(nomesSugeridos(avaliacao)).doesNotContain(VIAGEM);
    assertThat(avaliacao.listaCompleta().stream().map(Item::nome)).doesNotContain(VIAGEM);
  }

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
  @DisplayName(
      "Viagem PARCIAL (150) e 600 QUERO: falta 500; a viagem entra no corte só pelo restante de 150 (CA10)")
  void itemParcialEntraNoCortePeloRestanteDoPlanejado() {
    Avaliacao avaliacao =
        avaliar(comViagem(viagem(StatusItem.PARCIAL, "150")), "600.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("500");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, BOLA, ITEM_CASA, VIAGEM);
    assertThat(sugestao.cobre()).isEqualByComparingTo("550");
    Item viagemSugerida = sugestao.itens().get(3);
    assertThat(viagemSugerida.valorDeCorte()).isEqualByComparingTo("150");
    assertThat(viagemSugerida.jaSaiu()).isEqualByComparingTo("150");
  }

  @Test
  @DisplayName(
      "Cartão protegido, 1100 QUERO_FAZER: mesmo resultado do CA15, sem o cartão nas sugestões nem na lista completa (CA11)")
  void cartaoProtegidoNuncaApareceComoSugestaoDeCorte() {
    Avaliacao avaliacao = avaliar(base(), "1100.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("1000");
    assertThat(avaliacao.sugestoes()).isEmpty();
    assertThat(nomesSugeridos(avaliacao)).doesNotContain(CARTAO);
    assertThat(avaliacao.listaCompleta().stream().map(Item::nome))
        .containsExactly(JANTAR, BOLA, ITEM_CASA, VIAGEM, MANUTENCAO)
        .doesNotContain(CARTAO);
    assertThat(avaliacao.aindaFaltaria()).isEqualByComparingTo("100");
  }

  @Test
  @DisplayName(
      "900 QUERO_FAZER, falta 800: manutenção entra como último recurso e a bola fica de fora (CA14)")
  void compraQueroFazerUsaDevoFazerComoUltimoRecursoECortaSoOMinimoDosDemais() {
    Avaliacao avaliacao = avaliar(base(), "900.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("800");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(sugestao.opcao()).isEqualTo(Opcao.PADRAO);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, ITEM_CASA, VIAGEM, MANUTENCAO);
    assertThat(sugestao.cobre()).isEqualByComparingTo("820");
    assertThat(avaliacao.listaCompleta()).isEmpty();
  }

  @Test
  @DisplayName(
      "1100 QUERO_FAZER, falta 1000: sem sugestão, lista completa em ordem e ainda faltariam 100 (CA15)")
  void compraQueroFazerSemCoberturaTrazListaCompletaComDevoFazerEQuantoAindaFaltaria() {
    Avaliacao avaliacao = avaliar(base(), "1100.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("1000");
    assertThat(avaliacao.sugestoes()).isEmpty();
    assertThat(avaliacao.listaCompleta().stream().map(Item::nome))
        .containsExactly(JANTAR, BOLA, ITEM_CASA, VIAGEM, MANUTENCAO);
    assertThat(avaliacao.aindaFaltaria()).isEqualByComparingTo("100");
  }

  @Test
  @DisplayName(
      "500 SERIA_LEGAL é NAO_CABE por falta de itens seria legal; falta 200 depois dos cortes (CA16)")
  void compraSeriaLegalSemItensSuficientesInformaOQueFaltariaDepoisDosCortes() {
    Avaliacao avaliacao = avaliar(base(), "500.00", Categoria.SERIA_LEGAL);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.NAO_CABE);
    assertThat(avaliacao.motivo()).isEqualTo(Motivo.FALTAM_ITENS_SERIA_LEGAL);
    assertThat(avaliacao.falta()).isEqualByComparingTo("200");
    assertThat(avaliacao.sugestoes()).isEmpty();
  }

  @Test
  @DisplayName(
      "600 QUERO_FAZER, falta 500: jantar, bola e só a viagem; o item para casa não entra (CA19)")
  void doGrupoQueroFazerEntraSoOMinimoQueOsSeriaLegalNaoCobrem() {
    Avaliacao avaliacao = avaliar(base(), "600.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("500");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, BOLA, VIAGEM);
    assertThat(sugestao.cobre()).isEqualByComparingTo("500");
  }

  @Test
  @DisplayName(
      "400 QUERO_FAZER, falta 300: jantar e item para casa (320); a viagem sozinha não é sugerida (CA20)")
  void menosImportanteSaiPrimeiroMesmoQuandoUmItemMaisImportanteCobririaSozinho() {
    Avaliacao avaliacao = avaliar(base(), "400.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("300");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, ITEM_CASA);
    assertThat(sugestao.cobre()).isEqualByComparingTo("320");
  }
}
