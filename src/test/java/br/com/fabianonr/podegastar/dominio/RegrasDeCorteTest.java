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
      "Compra SERIA_LEGAL que os SERIA_LEGAL não cobrem é NAO_CABE, sem trocar por QUERO (R6)")
  void compraSeriaLegalSemCoberturaDeSeriaLegalNaoCabe() {
    // livre 100, falta 900; os SERIA_LEGAL cobrem só 200.
    Avaliacao avaliacao = avaliar("1000.00", Categoria.SERIA_LEGAL);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.NAO_CABE);
    assertThat(avaliacao.motivo()).isEqualTo(Motivo.FALTAM_ITENS_SERIA_LEGAL);
    assertThat(avaliacao.sugestoes()).isEmpty();
  }

  @Test
  @DisplayName("Opção 1: todos SERIA_LEGAL e QUERO_FAZER mais um DEVO_FAZER que fecha o valor (R7)")
  void compraDevoFazerSemCoberturaUsaOpcaoComUmDevoFazerQueFechaOValor() {
    // falta 900; SERIA + QUERO cobrem 700; a manutenção (200) fecha. O cartão nunca entra.
    Avaliacao avaliacao = avaliar("1000.00", Categoria.DEVO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("900");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(sugestao.opcao()).isEqualTo(Opcao.OPCAO_1);
    assertThat(nomes(sugestao))
        .containsExactlyInAnyOrder(JANTAR, BOLA, VIAGEM, ITEM_CASA, MANUTENCAO);
    assertThat(sugestao.cobre()).isEqualByComparingTo("900");
    assertThat(avaliacao.listaCompleta()).isEmpty();
  }

  @Test
  @DisplayName("Sem nenhuma opção que cubra, lista completa de elegíveis e quanto ainda falta (R7)")
  void compraDevoFazerSemOpcaoQueCubraTrazListaCompletaEQuantoAindaFaltaria() {
    // falta 1500; elegíveis (sem o cartão) somam 900.
    Avaliacao avaliacao = avaliar("1600.00", Categoria.DEVO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.sugestoes()).isEmpty();
    assertThat(avaliacao.listaCompleta().stream().map(Item::nome))
        .containsExactlyInAnyOrder(JANTAR, BOLA, VIAGEM, ITEM_CASA, MANUTENCAO);
    assertThat(avaliacao.aindaFaltaria()).isEqualByComparingTo("600");
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
    assertThat(avaliacao.aindaFaltaria()).isEqualByComparingTo("550");
  }
}
