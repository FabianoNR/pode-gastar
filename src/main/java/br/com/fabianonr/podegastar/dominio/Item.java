package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;

public record Item(
    String nome,
    Categoria categoria,
    int intensidade,
    boolean planejado,
    StatusItem status,
    BigDecimal valorEstimado,
    BigDecimal valorRealizado,
    String decisorResponsavel) {

  /** O cartão de crédito nunca é sugerido para corte (design aprovado, R5). */
  public static final String NOME_CARTAO = "Cartão de crédito";

  /** Valor que o item reserva no mês. */
  public BigDecimal comprometido() {
    return switch (status) {
      case PENDENTE, EM_DISCUSSAO -> estimado();
      case PARCIAL -> estimado().max(realizado());
      case REALIZADO -> realizado();
    };
  }

  /** Valor que já saiu da conta. */
  public BigDecimal jaSaiu() {
    return switch (status) {
      case PARCIAL, REALIZADO -> realizado();
      case PENDENTE, EM_DISCUSSAO -> BigDecimal.ZERO;
    };
  }

  /** Quanto o livre aumenta se o item for cortado (para PARCIAL, só o restante). */
  public BigDecimal valorDeCorte() {
    return comprometido().subtract(jaSaiu());
  }

  /** Nunca REALIZADO, nunca o cartão de crédito (R5). */
  public boolean elegivelParaCorte() {
    return status != StatusItem.REALIZADO && !NOME_CARTAO.equals(nome);
  }

  private BigDecimal estimado() {
    return valorEstimado == null ? BigDecimal.ZERO : valorEstimado;
  }

  private BigDecimal realizado() {
    return valorRealizado == null ? BigDecimal.ZERO : valorRealizado;
  }
}
