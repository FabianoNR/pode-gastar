package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.ItemDeTeste.umItem;
import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Item protegido de corte (R5): a marcação do item, e não o nome, o tira das sugestões. */
class ItemProtegidoDeCorteTest {

  private static Avaliacao avaliar(Mes mes, String valor, Categoria categoria) {
    return mes.avaliar(new Compra(valor(valor), categoria));
  }

  private static List<String> nomes(Sugestao sugestao) {
    return sugestao.itens().stream().map(Item::nome).toList();
  }

  private static List<String> nomes(List<Item> itens) {
    return itens.stream().map(Item::nome).toList();
  }

  private static Item jantarDeCem() {
    return umItem("item-1", JANTAR, Categoria.SERIA_LEGAL, 2, "100").criar();
  }

  private static ItemDeTeste itemChamadoCartaoDeTrezentos() {
    return umItem("item-2", CARTAO, Categoria.QUERO_FAZER, 5, "300");
  }

  /** Orçamento variável 500, jantar fora (100) e um item chamado Cartão de crédito (300). */
  private static Mes mesComJantarE(Item itemChamadoCartao) {
    return new Mes(
        MES_ID, valor("500"), valor("0"), valor("0"), List.of(jantarDeCem(), itemChamadoCartao));
  }

  @Test
  @DisplayName(
      "Sem a marcação, o item chamado Cartão de crédito é sugerido e cobre a falta sozinho (CA60)")
  void nomeSozinhoNaoProtegeOItem() {
    Mes mes = mesComJantarE(itemChamadoCartaoDeTrezentos().criar());
    assertThat(mes.livre()).isEqualByComparingTo("100");
    assertThat(mes.totalRestante()).isEqualByComparingTo("500");

    Avaliacao avaliacao = avaliar(mes, "300.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("200");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(nomes(sugestao)).containsExactly(CARTAO);
    assertThat(sugestao.cobre()).isEqualByComparingTo("300");
  }

  @Test
  @DisplayName(
      "Item protegido não é sugerido: sugestões vazias, lista completa só com o jantar (CA61)")
  void itemProtegidoNaoEntraNaSugestaoNemNaListaCompleta() {
    Mes mes = mesComJantarE(itemChamadoCartaoDeTrezentos().protegidoDeCorte().criar());
    assertThat(mes.livre()).isEqualByComparingTo("100");
    assertThat(mes.totalRestante()).isEqualByComparingTo("500");

    Avaliacao avaliacao = avaliar(mes, "300.00", Categoria.QUERO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("200");
    assertThat(avaliacao.sugestoes()).isEmpty();
    assertThat(nomes(avaliacao.listaCompleta())).containsExactly(JANTAR);
    assertThat(avaliacao.aindaFaltaria()).isEqualByComparingTo("100");
  }

  @Test
  @DisplayName("Bolsa em discussão protegida não é sugerida; jantar e bola cobrem 200 (CA62)")
  void itemEmDiscussaoProtegidoNaoEntraNoGrupoZero() {
    Item bolsaProtegida =
        umItem("item-7", BOLSA, Categoria.QUERO_FAZER, 3, "90")
            .emDiscussaoAguardando("marido")
            .protegidoDeCorte()
            .criar();
    Mes mes = comExtras(bolsaProtegida);
    assertThat(mes.livre()).isEqualByComparingTo("10");
    assertThat(mes.totalRestante()).isEqualByComparingTo("1600");

    Avaliacao avaliacao = avaliar(mes, "150.00", Categoria.SERIA_LEGAL);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("140");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, BOLA);
    assertThat(sugestao.cobre()).isEqualByComparingTo("200");
  }

  @Test
  @DisplayName("Com o cartão protegido, o mês base mantém livre 100 e total restante 1600 (CA63)")
  void marcacaoDoCartaoNaoMudaOCalculo() {
    Mes mes = base();

    assertThat(mes.itens())
        .filteredOn(Item::protegidoDeCorte)
        .extracting(Item::nome)
        .containsExactly(CARTAO);
    assertThat(mes.livre()).isEqualByComparingTo("100");
    assertThat(mes.totalRestante()).isEqualByComparingTo("1600");
  }

  @Test
  @DisplayName(
      "1000 DEVO_FAZER, falta 900: o cartão protegido não entra em nenhuma opção; sobra só a opção 1")
  void itemProtegidoNaoEntraNasOpcoesDaCompraDevoFazer() {
    Avaliacao avaliacao = avaliar(base(), "1000.00", Categoria.DEVO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("900");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(sugestao.opcao()).isEqualTo(Opcao.OPCAO_1);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, BOLA, ITEM_CASA, VIAGEM, MANUTENCAO);
  }
}
