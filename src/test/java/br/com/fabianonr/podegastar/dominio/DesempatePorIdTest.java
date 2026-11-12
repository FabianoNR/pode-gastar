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

  private static Mes mesCom(String renda, Item... itens) {
    return new Mes(MES_ID, valor(renda), valor("0"), valor("0"), List.of(itens));
  }

  @Test
  @DisplayName(
      "Combinações empatadas em intensidade e valor são desempatadas pelo menor id, não pelo nome")
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
  @DisplayName("Opções com itens de mesmo nome, mas ids diferentes, não se fundem")
  void opcoesComMesmoNomeEIdsDiferentesNaoSeFundem() {
    // Dois itens "Conserto" diferentes: um mais barato e intenso, outro mais caro e leve.
    Mes mes =
        mesCom(
            "2000",
            pendente("item-1", "Jantar", Categoria.SERIA_LEGAL, 2, "100"),
            pendente("item-2", "Conserto", Categoria.DEVO_FAZER, 5, "650"),
            pendente("item-3", "Conserto", Categoria.DEVO_FAZER, 1, "700"));

    // livre 550; falta 600; o seria legal cobre 100 e faltam 500 para o último recurso (R7).
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("1150.00"), Categoria.DEVO_FAZER));

    assertThat(avaliacao.sugestoes()).hasSize(2);
    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-1", "item-2");
    assertThat(ids(avaliacao.sugestoes().get(1))).containsExactly("item-1", "item-3");
  }
}
