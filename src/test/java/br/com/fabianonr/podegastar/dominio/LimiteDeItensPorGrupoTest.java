package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Limite de 20 itens por grupo nas sugestões de corte (ADR 0012). Itens em discussão e DEVO_FAZER
 * da compra QUERO_FAZER são conferidos antes de qualquer combinação, mesmo que o grupo não seja
 * usado.
 */
class LimiteDeItensPorGrupoTest {

  private static final int LIMITE = 20;
  private static final int ACIMA_DO_LIMITE = 21;
  private static final int MUITO_ACIMA_DO_LIMITE = 64;

  private static List<Item> itensEmDiscussao(int quantidade) {
    return itens(
        quantidade,
        numero ->
            emDiscussao(
                "item-" + numero, "Pedido " + numero, Categoria.SERIA_LEGAL, 1, "10", "marido"));
  }

  private static List<Item> itensPendentes(int quantidade, Categoria categoria, String prefixo) {
    return itens(
        quantidade,
        numero -> pendente(prefixo + numero, "Item " + prefixo + numero, categoria, 1, "10"));
  }

  private static Item cartaoProtegidoDeCorte(String valor) {
    return ItemDeTeste.umItem("cartao", CARTAO, Categoria.DEVO_FAZER, 5, valor)
        .protegidoDeCorte()
        .criar();
  }

  private static List<Item> itens(int quantidade, IntFunction<Item> criarItem) {
    return IntStream.rangeClosed(1, quantidade).mapToObj(criarItem).toList();
  }

  private static Mes mesCom(String orcamentoVariavel, List<Item> itens) {
    return new Mes(MES_ID, valor(orcamentoVariavel), valor("0"), valor("0"), itens);
  }

  /** Compra QUERO_FAZER do valor informado, num mês de orçamento variável 1000. */
  private static Avaliacao avaliarCompraQueroFazer(List<Item> itens, String valor) {
    return mesCom("1000", itens).avaliar(new Compra(valor(valor), Categoria.QUERO_FAZER));
  }

  private static List<String> ids(Sugestao sugestao) {
    return sugestao.itens().stream().map(Item::id).toList();
  }

  @Test
  @DisplayName("20 itens em discussão ainda geram a sugestão de corte (R6)")
  void vinteItensEmDiscussaoGeramSugestao() {
    // 20 itens de 10: livre 800; falta 10; um item em discussão cobre.
    Avaliacao avaliacao = avaliarCompraQueroFazer(itensEmDiscussao(LIMITE), "810.00");

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-1");
  }

  @Test
  @DisplayName("21 itens em discussão respondem itens demais (R6)")
  void vinteEUmItensEmDiscussaoSaoItensDemais() {
    List<Item> itens = itensEmDiscussao(ACIMA_DO_LIMITE);

    assertThatThrownBy(() -> avaliarCompraQueroFazer(itens, "800.00"))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }

  @Test
  @DisplayName(
      "21 itens em discussão, um deles protegido de corte, não passam do limite do grupo 0 (R5)")
  void itemEmDiscussaoProtegidoNaoContaNoLimite() {
    List<Item> itens = new ArrayList<>(itensEmDiscussao(ACIMA_DO_LIMITE));
    itens.set(
        0,
        ItemDeTeste.umItem("item-1", "Pedido 1", Categoria.SERIA_LEGAL, 1, "10")
            .emDiscussaoAguardando("marido")
            .protegidoDeCorte()
            .criar());

    // 21 itens de 10: livre 790; falta 10; um dos 20 itens não protegidos cobre.
    Avaliacao avaliacao = avaliarCompraQueroFazer(itens, "800.00");

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-2");
  }

  @Test
  @DisplayName(
      "21 itens em discussão respondem itens demais mesmo quando não há combinação a buscar (R6)")
  void itensEmDiscussaoAcimaDoLimiteSaoConferidosAntesDeCombinar() {
    List<Item> itens = new ArrayList<>(itensEmDiscussao(ACIMA_DO_LIMITE));
    itens.add(cartaoProtegidoDeCorte("500"));

    // Orçamento 1500, comprometido 710: livre 790; falta 510. Os elegíveis somam 210, então
    // sairia a lista completa, sem busca de combinações.
    Mes mes = mesCom("1500", itens);

    assertThatThrownBy(() -> mes.avaliar(new Compra(valor("1300.00"), Categoria.QUERO_FAZER)))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }

  @Test
  @DisplayName(
      "21 itens em discussão respondem itens demais na compra SERIA_LEGAL que não teria cortes suficientes (R6)")
  void itensEmDiscussaoAcimaDoLimiteSaoConferidosNaCompraSeriaLegal() {
    List<Item> itens = new ArrayList<>(itensEmDiscussao(ACIMA_DO_LIMITE));
    itens.add(cartaoProtegidoDeCorte("500"));

    // Orçamento 1500, comprometido 710: livre 790; falta 510. Os grupos 0 e 1 somam 210, então
    // sem o limite a decisão seria NAO_CABE por falta de itens seria legal.
    Mes mes = mesCom("1500", itens);

    assertThatThrownBy(() -> mes.avaliar(new Compra(valor("1300.00"), Categoria.SERIA_LEGAL)))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }

  @Test
  @DisplayName("20 DEVO_FAZER ainda geram a sugestão de corte da compra QUERO_FAZER (R6)")
  void vinteDevoFazerGeramSugestaoNaCompraQueroFazer() {
    // 20 itens de 10: livre 800; falta 10; só o grupo DEVO_FAZER pode cobrir.
    Avaliacao avaliacao =
        avaliarCompraQueroFazer(itensPendentes(LIMITE, Categoria.DEVO_FAZER, "item-"), "810.00");

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-1");
  }

  @Test
  @DisplayName("21 DEVO_FAZER respondem itens demais na compra QUERO_FAZER (R6)")
  void vinteEUmDevoFazerSaoItensDemaisNaCompraQueroFazer() {
    List<Item> itens = itensPendentes(ACIMA_DO_LIMITE, Categoria.DEVO_FAZER, "item-");

    assertThatThrownBy(() -> avaliarCompraQueroFazer(itens, "800.00"))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }

  @Test
  @DisplayName(
      "21 DEVO_FAZER respondem itens demais na compra QUERO_FAZER mesmo sem o grupo ser usado (R6)")
  void devoFazerAcimaDoLimiteSaoConferidosMesmoSemSerUsados() {
    List<Item> itens =
        new ArrayList<>(itensPendentes(ACIMA_DO_LIMITE, Categoria.DEVO_FAZER, "devo-"));
    itens.add(pendente("jantar", JANTAR, Categoria.SERIA_LEGAL, 2, "100"));

    // Orçamento 1000, comprometido 310: livre 690; falta 10. O jantar cobre sozinho.
    assertThatThrownBy(() -> avaliarCompraQueroFazer(itens, "700.00"))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }

  @Test
  @DisplayName("21 QUERO_FAZER respondem itens demais quando o grupo precisa ser combinado (R6)")
  void vinteEUmQueroFazerSaoItensDemais() {
    List<Item> itens = itensPendentes(ACIMA_DO_LIMITE, Categoria.QUERO_FAZER, "item-");

    assertThatThrownBy(() -> avaliarCompraQueroFazer(itens, "800.00"))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }

  @Test
  @DisplayName("64 itens em discussão respondem itens demais, nunca erro interno (R6)")
  void sessentaEQuatroItensEmDiscussaoSaoItensDemais() {
    List<Item> itens = itensEmDiscussao(MUITO_ACIMA_DO_LIMITE);

    assertThatThrownBy(() -> avaliarCompraQueroFazer(itens, "1000.00"))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }

  @Test
  @DisplayName(
      "64 DEVO_FAZER respondem itens demais na compra QUERO_FAZER, nunca erro interno (R6)")
  void sessentaEQuatroDevoFazerSaoItensDemais() {
    List<Item> itens = itensPendentes(MUITO_ACIMA_DO_LIMITE, Categoria.DEVO_FAZER, "item-");

    assertThatThrownBy(() -> avaliarCompraQueroFazer(itens, "1000.00"))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }

  @Test
  @DisplayName("Na opção 2, QUERO_FAZER e DEVO_FAZER juntos continuam limitados a 20 itens (R7)")
  void opcaoDoisContaQueroEDevoJuntosNoLimite() {
    List<Item> itensDoMes = new ArrayList<>();
    itensDoMes.addAll(itensPendentes(11, Categoria.QUERO_FAZER, "quero-"));
    itensDoMes.addAll(itensPendentes(10, Categoria.DEVO_FAZER, "devo-"));

    // 21 itens de 10: livre 790; falta 200; os QUERO (110) não cobrem e a compra segue para as
    // opções. Cada grupo tem menos de 20 itens, mas a opção 2 combina os 21.
    Mes mes = mesCom("1000", itensDoMes);

    assertThatThrownBy(() -> mes.avaliar(new Compra(valor("990.00"), Categoria.DEVO_FAZER)))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }
}
