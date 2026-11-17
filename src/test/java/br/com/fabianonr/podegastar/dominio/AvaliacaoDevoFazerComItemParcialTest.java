package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Critérios CA17 e CA18 da spec (v8): compra DEVO_FAZER em meses com um DEVO_FAZER PARCIAL. */
class AvaliacaoDevoFazerComItemParcialTest {

  private static final String DENTISTA = "Dentista";

  private static Mes mesCom(String orcamentoVariavel, Item... itens) {
    return new Mes(MES_ID, valor(orcamentoVariavel), valor("0"), valor("0"), List.of(itens));
  }

  private static Item dentistaParcial(String id) {
    return new Item(
        id,
        DENTISTA,
        Categoria.DEVO_FAZER,
        2,
        true,
        StatusItem.PARCIAL,
        valor("300"),
        valor("100"),
        null);
  }

  private static List<String> nomes(Sugestao sugestao) {
    return sugestao.itens().stream().map(Item::nome).toList();
  }

  @Test
  @DisplayName(
      "600 DEVO_FAZER, falta 500: jantar, bola e o dentista PARCIAL pelo cortável de 200 (CA17)")
  void devoFazerParcialEntraNasOpcoesPeloCortavel() {
    Mes mes =
        mesCom(
            "700",
            pendente("item-1", JANTAR, Categoria.SERIA_LEGAL, 2, "200"),
            pendente("item-2", BOLA, Categoria.QUERO_FAZER, 2, "100"),
            dentistaParcial("item-3"));
    assertThat(mes.livre()).isEqualByComparingTo("100");
    assertThat(mes.totalRestante()).isEqualByComparingTo("600");

    Avaliacao avaliacao = mes.avaliar(new Compra(valor("600.00"), Categoria.DEVO_FAZER));

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("500");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, BOLA, DENTISTA);
    assertThat(sugestao.cobre()).isEqualByComparingTo("500");
    assertThat(avaliacao.listaCompleta()).isEmpty();
  }

  @Test
  @DisplayName(
      "400 DEVO_FAZER, falta 300: jantar e dentista; o dentista vence a manutenção pela intensidade (CA18)")
  void entreDevoFazerDeMesmoCortavelEntraODeMenorIntensidade() {
    Mes mes =
        mesCom(
            "800",
            pendente("item-1", JANTAR, Categoria.SERIA_LEGAL, 2, "200"),
            pendente("item-2", MANUTENCAO, Categoria.DEVO_FAZER, 5, "200"),
            dentistaParcial("item-3"));
    assertThat(mes.livre()).isEqualByComparingTo("100");
    assertThat(mes.totalRestante()).isEqualByComparingTo("700");

    Avaliacao avaliacao = mes.avaliar(new Compra(valor("400.00"), Categoria.DEVO_FAZER));

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("300");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, DENTISTA);
    assertThat(sugestao.cobre()).isEqualByComparingTo("400");
  }
}
