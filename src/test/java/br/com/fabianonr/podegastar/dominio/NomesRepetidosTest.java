package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NomesRepetidosTest {

  private static Mes mesCom(Item... itens) {
    return new Mes(MES_ID, valor("1000"), valor("0"), valor("0"), List.of(itens));
  }

  @Test
  @DisplayName("Nomes que diferem só em maiúsculas e espaços nas pontas são nomes repetidos")
  void nomesQueDiferemSoEmMaiusculasEEspacosSaoRepetidos() {
    Mes mes =
        mesCom(
            pendente("item-9", " jantar FORA ", Categoria.SERIA_LEGAL, 2, "100"),
            pendente("item-5", "Jantar fora", Categoria.SERIA_LEGAL, 2, "120"),
            pendente("item-6", "Bola de futebol", Categoria.SERIA_LEGAL, 2, "80"));

    assertThat(mes.nomesRepetidos())
        .containsExactly(
            new NomeRepetido(List.of("Jantar fora", " jantar FORA "), List.of("item-5", "item-9")));
  }

  @Test
  @DisplayName("Sem nomes repetidos, a avaliação não traz nenhum nome repetido")
  void semNomesRepetidosNaoHaAviso() {
    Avaliacao avaliacao = base().avaliar(new Compra(valor("80.00"), Categoria.QUERO_FAZER));

    assertThat(avaliacao.nomesRepetidos()).isEmpty();
  }

  @Test
  @DisplayName("Itens com o mesmo nome continuam separados: os dois entram na sugestão e no aviso")
  void nomeRepetidoNaoRemoveNemEscondeItens() {
    Mes mes =
        mesCom(
            pendente("item-1", "Bola", Categoria.SERIA_LEGAL, 2, "80"),
            pendente("item-2", "Bola", Categoria.SERIA_LEGAL, 2, "80"));

    // livre 840; falta 150: só os dois itens juntos cobrem.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("990.00"), Categoria.SERIA_LEGAL));

    assertThat(mes.itens()).hasSize(2);
    assertThat(avaliacao.sugestoes().get(0).itens().stream().map(Item::id))
        .containsExactly("item-1", "item-2");
    assertThat(avaliacao.nomesRepetidos())
        .containsExactly(new NomeRepetido(List.of("Bola"), List.of("item-1", "item-2")));
  }
}
