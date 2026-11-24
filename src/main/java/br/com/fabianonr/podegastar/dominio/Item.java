package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;

public record Item(
    String id,
    String nome,
    Categoria categoria,
    int intensidade,
    boolean planejado,
    StatusItem status,
    BigDecimal valorEstimado,
    BigDecimal valorRealizado,
    String decisorResponsavel,
    boolean protegidoDeCorte) {

  public Item {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("item sem id");
    }
  }

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

  /** Nunca REALIZADO, nunca item protegido de corte, qualquer que seja o nome (R5). */
  public boolean elegivelParaCorte() {
    return status != StatusItem.REALIZADO && !protegidoDeCorte;
  }

  private BigDecimal estimado() {
    return valorEstimado == null ? BigDecimal.ZERO : valorEstimado;
  }

  private BigDecimal realizado() {
    return valorRealizado == null ? BigDecimal.ZERO : valorRealizado;
  }
}
