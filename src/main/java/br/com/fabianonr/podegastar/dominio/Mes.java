package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public record Mes(
    String id, BigDecimal renda, BigDecimal custoFixo, BigDecimal investimento, List<Item> itens) {

  public Mes {
    itens = List.copyOf(itens);
  }

  /** Renda, menos custo fixo, menos investimento. */
  public BigDecimal orcamentoVariavel() {
    return renda.subtract(custoFixo).subtract(investimento);
  }

  /** Orçamento variável, menos a soma dos valores comprometidos. */
  public BigDecimal livre() {
    return orcamentoVariavel().subtract(somar(Item::comprometido));
  }

  /** Orçamento variável, menos a soma dos valores já saídos. */
  public BigDecimal totalRestante() {
    return orcamentoVariavel().subtract(somar(Item::jaSaiu));
  }

  public Avaliacao avaliar(Compra compra) {
    BigDecimal livre = livre();
    BigDecimal total = totalRestante();
    BigDecimal valor = compra.valor();

    if (valor.compareTo(livre) <= 0) {
      return Avaliacao.aprovado(livre, total, livre.subtract(valor));
    }
    if (valor.compareTo(total) > 0) {
      return Avaliacao.naoCabe(livre, total, valor.subtract(total), Avaliacao.MOTIVO_NAO_CABE);
    }
    return Avaliacao.cabeComCortes(
        livre,
        total,
        valor.subtract(livre),
        Avaliacao.MOTIVO_CABE_COM_CORTES,
        List.of(),
        List.of());
  }

  private BigDecimal somar(Function<Item, BigDecimal> valor) {
    return itens.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
