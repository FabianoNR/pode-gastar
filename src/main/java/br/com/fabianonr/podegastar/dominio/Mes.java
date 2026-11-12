package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public record Mes(
    String id, BigDecimal renda, BigDecimal custoFixo, BigDecimal investimento, List<Item> itens) {

  public Mes {
    itens = List.copyOf(itens);
    exigirIdsUnicos(itens);
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

  /** Decide a compra e acrescenta os nomes repetidos do mês, sem remover nenhum item. */
  public Avaliacao avaliar(Compra compra) {
    return decidir(compra).comNomesRepetidos(nomesRepetidos());
  }

  /** Itens com o mesmo nome no mês, ignorando maiúsculas e espaços nas pontas. */
  public List<NomeRepetido> nomesRepetidos() {
    return NomeRepetido.encontrarEm(itens);
  }

  private Avaliacao decidir(Compra compra) {
    BigDecimal livre = livre();
    BigDecimal total = totalRestante();
    BigDecimal valor = compra.valor();

    if (valor.compareTo(livre) <= 0) {
      return Avaliacao.aprovado(livre, total, livre.subtract(valor));
    }
    if (valor.compareTo(total) > 0) {
      return Avaliacao.naoCabe(
          livre, total, valor.subtract(total), Motivo.COMPRA_EXCEDE_TOTAL_RESTANTE);
    }
    return PlanoDeCortes.montar(itens, compra, livre, total, valor.subtract(livre));
  }

  private static void exigirIdsUnicos(List<Item> itens) {
    Set<String> idsVistos = new HashSet<>();
    for (Item item : itens) {
      if (!idsVistos.add(item.id())) {
        throw new IllegalArgumentException("id de item repetido no mês: " + item.id());
      }
    }
  }

  private BigDecimal somar(Function<Item, BigDecimal> valor) {
    return itens.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
