package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdentidadeDoItemTest {

  @Test
  @DisplayName("Item sem id é recusado")
  void recusaItemSemId() {
    assertThatThrownBy(() -> pendente(" ", BOLA, Categoria.SERIA_LEGAL, 2, "80"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Mês recusa dois itens com o mesmo id, porque o id é único dentro do mês")
  void recusaIdRepetidoNoMes() {
    List<Item> itensComIdRepetido =
        List.of(
            pendente("item-1", BOLA, Categoria.SERIA_LEGAL, 2, "80"),
            pendente("item-1", JANTAR, Categoria.SERIA_LEGAL, 2, "120"));

    assertThatThrownBy(
            () -> new Mes(MES_ID, valor("1000"), valor("0"), valor("0"), itensComIdRepetido))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("item-1");
  }
}
