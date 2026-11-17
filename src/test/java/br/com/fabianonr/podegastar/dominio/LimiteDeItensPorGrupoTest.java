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
 * Onde o limite de 20 itens por grupo vale (decisão de 17/11/2026, registrada na PR #20): nos
 * grupos SERIA_LEGAL e QUERO_FAZER da regra de corte e na opção 2, com QUERO_FAZER e DEVO_FAZER
 * contados juntos. Não vale para os itens em discussão nem para o DEVO_FAZER da compra QUERO_FAZER.
 */
class LimiteDeItensPorGrupoTest {

  private static final int ITENS_ACIMA_DO_LIMITE = 21;

  private static List<Item> itens(int quantidade, IntFunction<Item> criarItem) {
    return IntStream.rangeClosed(1, quantidade).mapToObj(criarItem).toList();
  }

  private static Mes mesCom(String orcamentoVariavel, List<Item> itens) {
    return new Mes(MES_ID, valor(orcamentoVariavel), valor("0"), valor("0"), itens);
  }

  private static List<String> ids(Sugestao sugestao) {
    return sugestao.itens().stream().map(Item::id).toList();
  }

  @Test
  @DisplayName("Mais de 20 itens em discussão não impedem a sugestão de corte (R6)")
  void itensEmDiscussaoNaoTemLimiteDeItens() {
    List<Item> emDiscussao =
        itens(
            ITENS_ACIMA_DO_LIMITE,
            numero ->
                emDiscussao(
                    "item-" + numero,
                    "Pedido " + numero,
                    Categoria.SERIA_LEGAL,
                    1,
                    "10",
                    "marido"));

    // 21 itens de 10: livre 790; falta 10; um item em discussão cobre.
    Avaliacao avaliacao =
        mesCom("1000", emDiscussao).avaliar(new Compra(valor("800.00"), Categoria.QUERO_FAZER));

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.sugestoes()).hasSize(1);
    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-1");
  }

  @Test
  @DisplayName("Mais de 20 DEVO_FAZER não impedem a sugestão de corte da compra QUERO_FAZER (R6)")
  void devoFazerDaCompraQueroFazerNaoTemLimiteDeItens() {
    List<Item> devoFazer =
        itens(
            ITENS_ACIMA_DO_LIMITE,
            numero -> pendente("item-" + numero, "Conta " + numero, Categoria.DEVO_FAZER, 1, "10"));

    // 21 itens de 10: livre 790; falta 10; só o grupo DEVO_FAZER pode cobrir.
    Avaliacao avaliacao =
        mesCom("1000", devoFazer).avaliar(new Compra(valor("800.00"), Categoria.QUERO_FAZER));

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.sugestoes()).hasSize(1);
    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-1");
  }

  @Test
  @DisplayName("Na opção 2, QUERO_FAZER e DEVO_FAZER juntos continuam limitados a 20 itens (R7)")
  void opcaoDoisContaQueroEDevoJuntosNoLimite() {
    List<Item> itensDoMes = new ArrayList<>();
    itensDoMes.addAll(
        itens(
            11,
            numero ->
                pendente("quero-" + numero, "Desejo " + numero, Categoria.QUERO_FAZER, 1, "10")));
    itensDoMes.addAll(
        itens(
            10,
            numero ->
                pendente("devo-" + numero, "Conta " + numero, Categoria.DEVO_FAZER, 1, "10")));

    // 21 itens de 10: livre 790; falta 200; os QUERO (110) não cobrem e a compra segue para as
    // opções. Cada grupo tem menos de 20 itens, mas a opção 2 combina os 21.
    Mes mes = mesCom("1000", itensDoMes);

    assertThatThrownBy(() -> mes.avaliar(new Compra(valor("990.00"), Categoria.DEVO_FAZER)))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }
}
