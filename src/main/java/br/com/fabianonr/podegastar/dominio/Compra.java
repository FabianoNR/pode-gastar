package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;

/**
 * Simulação de um gasto novo.
 *
 * @param intensidadeInformada se o 1 veio de quem consultou ou é só o padrão (R12)
 */
public record Compra(
    BigDecimal valor, Categoria categoria, int intensidade, boolean intensidadeInformada) {

  private static final int INTENSIDADE_PADRAO = 1;
  private static final int INTENSIDADE_MAXIMA = 5;

  public Compra {
    exigirValorPositivo(valor);
    if (categoria == null) {
      throw new ValorInvalidoException("categoria", "categoria é obrigatória");
    }
    exigirIntensidadeDeUmACinco(intensidade);
  }

  /** Valor menor ou igual a zero é entrada inválida (R1). */
  public static void exigirValorPositivo(BigDecimal valor) {
    if (valor == null || valor.signum() <= 0) {
      throw new ValorInvalidoException("valor", "valor deve ser maior que zero");
    }
  }

  /** A intensidade da compra é um inteiro de 1 a 5 (R12). */
  public static void exigirIntensidadeDeUmACinco(int intensidade) {
    if (intensidade < INTENSIDADE_PADRAO || intensidade > INTENSIDADE_MAXIMA) {
      throw new ValorInvalidoException("intensidade", "intensidade deve ser um inteiro de 1 a 5");
    }
  }

  /** Compra sem intensidade informada: vale 1. */
  public Compra(BigDecimal valor, Categoria categoria) {
    this(valor, categoria, INTENSIDADE_PADRAO, false);
  }

  /** Compra com a intensidade informada por quem consultou. */
  public Compra(BigDecimal valor, Categoria categoria, int intensidade) {
    this(valor, categoria, intensidade, true);
  }
}
