package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Mes(
    String id, BigDecimal renda, BigDecimal custoFixo, BigDecimal investimento, List<Item> itens) {

  public BigDecimal livre() {
    throw new UnsupportedOperationException("não implementado");
  }

  public BigDecimal totalRestante() {
    throw new UnsupportedOperationException("não implementado");
  }

  public Avaliacao avaliar(Compra compra) {
    throw new UnsupportedOperationException("não implementado");
  }
}
