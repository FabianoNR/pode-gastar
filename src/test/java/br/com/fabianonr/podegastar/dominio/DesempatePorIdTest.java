package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DesempatePorIdTest {

  private static List<String> ids(Sugestao sugestao) {
    return sugestao.itens().stream().map(Item::id).toList();
  }

  private static Mes mesCom(String orcamentoVariavel, Item... itens) {
    return new Mes(MES_ID, valor(orcamentoVariavel), valor("0"), valor("0"), List.of(itens));
  }

  @Test
  @DisplayName(
      "Combinações empatadas em valor e intensidade são desempatadas pelo menor id, não pelo nome (R6)")
  void desempataCombinacoesPeloMenorId() {
    // Ordem por nome escolheria "A cesta"; ordem por id escolhe item-1 ("Zebra").
    Mes mes =
        mesCom(
            "1000",
            pendente("item-9", "A cesta", Categoria.SERIA_LEGAL, 2, "80"),
            pendente("item-1", "Zebra", Categoria.SERIA_LEGAL, 2, "80"));

    // livre 840; falta 50; cada item sozinho cobre.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("890.00"), Categoria.SERIA_LEGAL));

    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-1");
  }

  @Test
  @DisplayName(
      "No desempate por id entre combinações, os números do id contam pelo valor: item-2 antes de item-10 (R6)")
  void desempatePorIdUsaOrdemNumericaNatural() {
    // Na ordem alfabética, "item-10" viria antes de "item-2".
    Mes mes =
        mesCom(
            "1000",
            pendente("item-10", "Cinema", Categoria.SERIA_LEGAL, 2, "80"),
            pendente("item-2", "Livro", Categoria.SERIA_LEGAL, 2, "80"));

    // livre 840; falta 50; cada item sozinho cobre.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("890.00"), Categoria.SERIA_LEGAL));

    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-2");
  }

  @Test
  @DisplayName("Opções com itens de mesmo nome, mas ids diferentes, não se fundem (R7)")
  void opcoesComMesmoNomeEIdsDiferentesNaoSeFundem() {
    // Três itens "Conserto" diferentes. Pelo nome, as opções 1 e 2 seriam iguais.
    Mes mes =
        mesCom(
            "1400",
            pendente("item-1", "Jantar", Categoria.SERIA_LEGAL, 2, "100"),
            pendente("item-2", "Conserto", Categoria.DEVO_FAZER, 1, "300"),
            pendente("item-3", "Conserto", Categoria.DEVO_FAZER, 1, "300"),
            pendente("item-4", "Conserto", Categoria.DEVO_FAZER, 3, "600"));

    // livre 100; falta 600; depois do jantar faltam 500 e a compra segue para as opções (R7).
    // Opção 1: jantar e item-4, o único conserto que cobre 500 sozinho.
    // Opção 2: jantar, item-2 e item-3 (soma de intensidades 2 contra 3 do item-4).
    // Opção 3: jantar e item-4, igual à opção 1 pelos ids; sai.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("700.00"), Categoria.DEVO_FAZER));

    assertThat(avaliacao.sugestoes()).hasSize(2);
    assertThat(avaliacao.sugestoes().get(0).opcao()).isEqualTo(Opcao.OPCAO_1);
    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-1", "item-4");
    assertThat(avaliacao.sugestoes().get(1).opcao()).isEqualTo(Opcao.OPCAO_2);
    assertThat(ids(avaliacao.sugestoes().get(1))).containsExactly("item-1", "item-2", "item-3");
  }
}
