package br.com.fabianonr.podegastar.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DinheiroTest {

  @ParameterizedTest(name = "{0} vira {1}")
  @CsvSource({"0.125, 0.12", "0.135, 0.14", "2.675, 2.68", "2.665, 2.66"})
  @DisplayName("Valor exatamente no meio arredonda para o centavo par (ADR 0002)")
  void arredondaParaCentavoParQuandoEstaNoMeio(String valorOriginal, String textoEsperado) {
    assertThat(Dinheiro.formatar(new BigDecimal(valorOriginal))).isEqualTo(textoEsperado);
  }

  @ParameterizedTest(name = "{0} vira {1}")
  @CsvSource({"1600, 1600.00", "150.5, 150.50", "20.00, 20.00", "0, 0.00"})
  @DisplayName("Valor com até duas casas ganha duas casas e não muda de valor")
  void mantemOValorEFixaDuasCasas(String valorOriginal, String textoEsperado) {
    assertThat(Dinheiro.formatar(new BigDecimal(valorOriginal))).isEqualTo(textoEsperado);
  }
}
