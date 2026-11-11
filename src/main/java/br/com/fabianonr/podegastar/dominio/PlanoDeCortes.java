package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Monta as sugestões de corte (R5 e R6) para uma compra que não cabe no livre, mas cabe no total
 * restante.
 */
final class PlanoDeCortes {

  /** Limite da busca de combinações (2^n); o mês de uma pessoa tem poucos itens por grupo. */
  private static final int MAXIMO_ITENS_POR_GRUPO = 20;

  /** Categoria pesa antes da intensidade: seria legal, depois quero, depois devo. */
  private static final Comparator<Item> PRIORIDADE =
      Comparator.comparingInt((Item i) -> posicao(i.categoria()))
          .thenComparingInt(Item::intensidade)
          .thenComparing(Item::nome);

  private final List<Item> itens;
  private final Compra compra;
  private final BigDecimal livre;
  private final BigDecimal totalRestante;
  private final BigDecimal falta;
  private final List<Item> emDiscussao;

  private PlanoDeCortes(
      List<Item> itens,
      Compra compra,
      BigDecimal livre,
      BigDecimal totalRestante,
      BigDecimal falta) {
    this.itens = itens;
    this.compra = compra;
    this.livre = livre;
    this.totalRestante = totalRestante;
    this.falta = falta;
    this.emDiscussao =
        itens.stream()
            .filter(i -> i.status() == StatusItem.EM_DISCUSSAO && i.elegivelParaCorte())
            .sorted(PRIORIDADE)
            .toList();
  }

  static Avaliacao montar(
      List<Item> itens,
      Compra compra,
      BigDecimal livre,
      BigDecimal totalRestante,
      BigDecimal falta) {
    return new PlanoDeCortes(itens, compra, livre, totalRestante, falta).avaliar();
  }

  static BigDecimal somaDeCortes(List<Item> itens) {
    return itens.stream().map(Item::valorDeCorte).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private Avaliacao avaliar() {
    List<Item> escolhidos = new ArrayList<>();
    BigDecimal restante = falta;

    // 1. EM_DISCUSSAO, de qualquer categoria, na ordem de prioridade, até cobrir a falta.
    for (Item item : emDiscussao) {
      if (restante.signum() <= 0) {
        break;
      }
      escolhidos.add(item);
      restante = restante.subtract(item.valorDeCorte());
    }

    // 2. PENDENTE conforme a categoria da compra, grupo a grupo.
    for (List<Item> grupo : gruposDaCompra()) {
      if (restante.signum() <= 0) {
        break;
      }
      Optional<List<Item>> combinacao = melhorCombinacao(grupo, restante);
      List<Item> usados = combinacao.orElse(grupo);
      escolhidos.addAll(usados);
      restante = restante.subtract(somaDeCortes(usados));
    }

    if (restante.signum() <= 0) {
      return cabeComCortes(List.of(Sugestao.de(Opcao.PADRAO, escolhidos)), List.of(), null);
    }
    return semCobertura(restante);
  }

  /** Grupos de PENDENTE (ou PARCIAL) elegíveis, na ordem em que entram na sugestão. */
  private List<List<Item>> gruposDaCompra() {
    List<List<Item>> grupos = new ArrayList<>();
    grupos.add(candidatos(Categoria.SERIA_LEGAL));
    if (compra.categoria() != Categoria.SERIA_LEGAL) {
      grupos.add(candidatos(Categoria.QUERO_FAZER));
    }
    return grupos;
  }

  private List<Item> candidatos(Categoria categoria) {
    return itens.stream()
        .filter(i -> i.categoria() == categoria)
        .filter(i -> i.status() == StatusItem.PENDENTE || i.status() == StatusItem.PARCIAL)
        .filter(Item::elegivelParaCorte)
        .sorted(PRIORIDADE)
        .toList();
  }

  private Avaliacao semCobertura(BigDecimal aindaFalta) {
    List<Item> todos = new ArrayList<>(emDiscussao);
    todos.addAll(candidatos(Categoria.SERIA_LEGAL));
    if (compra.categoria() == Categoria.SERIA_LEGAL) {
      return Avaliacao.naoCabe(
          livre,
          totalRestante,
          aindaFalta,
          "não há itens seria legal suficientes para cobrir a compra");
    }
    todos.addAll(candidatos(Categoria.QUERO_FAZER));
    if (compra.categoria() == Categoria.DEVO_FAZER) {
      todos.addAll(candidatos(Categoria.DEVO_FAZER));
      aindaFalta = falta.subtract(somaDeCortes(todos));
    }
    return cabeComCortes(List.of(), todos, aindaFalta);
  }

  private Avaliacao cabeComCortes(
      List<Sugestao> sugestoes, List<Item> listaCompleta, BigDecimal aindaFalta) {
    String motivo =
        aindaFalta == null
            ? Avaliacao.MOTIVO_CABE_COM_CORTES
            : Avaliacao.MOTIVO_CABE_COM_CORTES
                + "; mesmo cortando todos os itens elegíveis ainda faltariam "
                + aindaFalta.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    return Avaliacao.cabeComCortes(livre, totalRestante, falta, motivo, sugestoes, listaCompleta);
  }

  /**
   * Entre as combinações do grupo que cobrem o alvo, a de menor intensidade e, depois, a de menor
   * valor. Combinação pode ter quantos itens forem necessários.
   */
  static Optional<List<Item>> melhorCombinacao(List<Item> grupo, BigDecimal alvo) {
    int n = grupo.size();
    if (n > MAXIMO_ITENS_POR_GRUPO) {
      throw new IllegalStateException("itens demais para combinar: " + n);
    }
    List<Item> melhor = null;
    for (int mascara = 1; mascara < (1 << n); mascara++) {
      List<Item> combinacao = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        if ((mascara & (1 << i)) != 0) {
          combinacao.add(grupo.get(i));
        }
      }
      if (somaDeCortes(combinacao).compareTo(alvo) >= 0
          && (melhor == null || COMBINACAO.compare(combinacao, melhor) < 0)) {
        melhor = combinacao;
      }
    }
    return Optional.ofNullable(melhor);
  }

  private static final Comparator<List<Item>> COMBINACAO =
      Comparator.<List<Item>>comparingInt(c -> c.stream().mapToInt(Item::intensidade).sum())
          .thenComparing(PlanoDeCortes::somaDeCortes)
          .thenComparing(c -> c.stream().map(Item::nome).toList().toString());

  private static int posicao(Categoria categoria) {
    return switch (categoria) {
      case SERIA_LEGAL -> 0;
      case QUERO_FAZER -> 1;
      case DEVO_FAZER -> 2;
    };
  }
}
