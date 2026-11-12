package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regra única de escala e arredondamento dos valores monetários (ADR 0002). */
public final class Dinheiro {

  public static final int ESCALA = 2;
  public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

  private Dinheiro() {}

  /** Texto do valor com duas casas decimais, arredondado com {@link #ARREDONDAMENTO}. */
  public static String formatar(BigDecimal valor) {
    return valor.setScale(ESCALA, ARREDONDAMENTO).toPlainString();
  }
}
