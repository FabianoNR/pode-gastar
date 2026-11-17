package br.com.fabianonr.podegastar.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrdemNaturalDeIdsTest {

  private static List<String> ordenar(String... ids) {
    return Stream.of(ids).sorted(OrdemNaturalDeIds::compararIds).toList();
  }

  @Test
  @DisplayName("Trechos numéricos do id comparam pelo valor: item-2 antes de item-10 (R6)")
  void trechosNumericosComparamPeloValor() {
    assertThat(ordenar("item-10", "item-9", "item-2", "item-1"))
        .containsExactly("item-1", "item-2", "item-9", "item-10");
  }

  @Test
  @DisplayName("Trechos de texto comparam pelo texto, e o prefixo mais curto vem antes (R6)")
  void trechosDeTextoComparamPeloTexto() {
    assertThat(ordenar("viagem", "item-3", "item", "devo-1"))
        .containsExactly("devo-1", "item", "item-3", "viagem");
  }

  @Test
  @DisplayName("Ids de mesmo valor com zeros à esquerda diferentes não empatam (R6)")
  void zerosAEsquerdaNaoEmpatam() {
    assertThat(OrdemNaturalDeIds.compararIds("item-02", "item-2")).isNotZero();
    assertThat(ordenar("item-2", "item-02")).containsExactly("item-02", "item-2");
  }
}
