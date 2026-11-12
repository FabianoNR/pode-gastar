package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regras de R6 e R7 que nenhum critério de aceite exercita. Os cenários são derivados do texto da
 * spec (v6) sobre o mês de exemplo base.
 */
class RegrasDeCorteTest {

  private static Avaliacao avaliar(String valor, Categoria categoria) {
    return base().avaliar(new Compra(valor(valor), categoria));
  }

  private static List<String> nomes(Sugestao sugestao) {
    return sugestao.itens().stream().map(Item::nome).toList();
  }

  @Test
  @DisplayName(
      "R6: compra SERIA_LEGAL que os SERIA_LEGAL não cobrem é NAO_CABE, sem trocar por QUERO")
  void seriaLegalSemCoberturaNaoCabe() {
    // livre 100, falta 900; os SERIA_LEGAL cobrem só 200.
    Avaliacao a = avaliar("1000.00", Categoria.SERIA_LEGAL);

    assertThat(a.decisao()).isEqualTo(Decisao.NAO_CABE);
    assertThat(a.motivo()).isEqualTo("não há itens seria legal suficientes para cobrir a compra");
    assertThat(a.sugestoes()).isEmpty();
  }

  @Test
  @DisplayName("R7 opção 1: todos SERIA_LEGAL e QUERO_FAZER mais um DEVO_FAZER que fecha o valor")
  void devoComOpcao1() {
    // falta 900; SERIA + QUERO cobrem 700; a manutenção (200) fecha. O cartão nunca entra.
    Avaliacao a = avaliar("1000.00", Categoria.DEVO_FAZER);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.falta()).isEqualByComparingTo("900");
    assertThat(a.sugestoes()).hasSize(1);
    Sugestao s = a.sugestoes().get(0);
    assertThat(s.opcao()).isEqualTo(Opcao.OPCAO_1);
    assertThat(nomes(s)).containsExactlyInAnyOrder(JANTAR, BOLA, VIAGEM, ITEM_CASA, MANUTENCAO);
    assertThat(s.cobre()).isEqualByComparingTo("900");
    assertThat(a.listaCompleta()).isEmpty();
  }

  @Test
  @DisplayName("R7: sem nenhuma opção que cubra, lista completa de elegíveis e quanto ainda falta")
  void devoSemNenhumaOpcao() {
    // falta 1500; elegíveis (sem o cartão) somam 900.
    Avaliacao a = avaliar("1600.00", Categoria.DEVO_FAZER);

    assertThat(a.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(a.sugestoes()).isEmpty();
    assertThat(a.listaCompleta().stream().map(Item::nome))
        .containsExactlyInAnyOrder(JANTAR, BOLA, VIAGEM, ITEM_CASA, MANUTENCAO);
    assertThat(a.motivo()).contains("ainda faltariam 600.00");
  }

  @Test
  @DisplayName(
      "Item parcial já gasto além do previsto continua comprometido, mas não aparece na sugestão de corte")
  void itemParcialGastoAlemDoPrevistoNaoApareceNaSugestaoDeCorte() {
    // Viagem estimada em 300 e já gasta em 350: reserva 350 e cortar não libera nada.
    Mes mes = comViagem(viagem(StatusItem.PARCIAL, "350"));
    assertThat(mes.livre()).isEqualByComparingTo("50");

    // falta 950; os itens que liberam valor somam 400, então sai a lista completa.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("1000.00"), Categoria.QUERO_FAZER));

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.listaCompleta().stream().map(Item::nome))
        .containsExactlyInAnyOrder(JANTAR, BOLA, ITEM_CASA);
    assertThat(avaliacao.motivo()).contains("ainda faltariam 550.00");
  }
}
