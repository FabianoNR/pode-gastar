package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;

public record Compra(BigDecimal valor, Categoria categoria) {

  public Compra {
    if (valor == null || valor.signum() <= 0) {
      throw new ValorInvalidoException("valor deve ser maior que zero");
    }
    if (categoria == null) {
      throw new ValorInvalidoException("categoria é obrigatória");
    }
  }
}
