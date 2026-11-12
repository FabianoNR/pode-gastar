package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

  /** Item que não libera valor ao ser cortado (PARCIAL já gasto além do previsto) fica de fora. */
  private List<Item> candidatos(Categoria categoria) {
    return itens.stream()
        .filter(i -> i.categoria() == categoria)
        .filter(i -> i.status() == StatusItem.PENDENTE || i.status() == StatusItem.PARCIAL)
        .filter(Item::elegivelParaCorte)
        .filter(item -> item.valorDeCorte().signum() > 0)
        .sorted(PRIORIDADE)
        .toList();
  }

  private Avaliacao semCobertura(BigDecimal aindaFalta) {
    if (compra.categoria() == Categoria.SERIA_LEGAL) {
      return Avaliacao.naoCabe(
          livre,
          totalRestante,
          aindaFalta,
          "não há itens seria legal suficientes para cobrir a compra");
    }
    if (compra.categoria() == Categoria.DEVO_FAZER) {
      List<Sugestao> opcoes = opcoesDeUltimoRecurso();
      if (!opcoes.isEmpty()) {
        return cabeComCortes(opcoes, List.of(), null);
      }
    }
    List<Item> todos = new ArrayList<>(emDiscussao);
    todos.addAll(candidatos(Categoria.SERIA_LEGAL));
    todos.addAll(candidatos(Categoria.QUERO_FAZER));
    if (compra.categoria() == Categoria.DEVO_FAZER) {
      todos.addAll(candidatos(Categoria.DEVO_FAZER));
    }
    return cabeComCortes(List.of(), todos, falta.subtract(somaDeCortes(todos)));
  }

  /**
   * R7: compra DEVO_FAZER que EM_DISCUSSAO, SERIA_LEGAL e QUERO_FAZER não cobrem. Os itens em
   * discussão entram sempre primeiro, como em R6.
   */
  private List<Sugestao> opcoesDeUltimoRecurso() {
    List<Item> seria = candidatos(Categoria.SERIA_LEGAL);
    List<Item> quero = candidatos(Categoria.QUERO_FAZER);
    List<Item> devo = candidatos(Categoria.DEVO_FAZER);
    BigDecimal aposDiscussao = falta.subtract(somaDeCortes(emDiscussao));
    BigDecimal aposSeria = aposDiscussao.subtract(somaDeCortes(seria));
    BigDecimal aposQuero = aposSeria.subtract(somaDeCortes(quero));

    Map<Set<String>, Sugestao> opcoes = new LinkedHashMap<>();

    // Opção 1: todos os seria legal, todos os quero e um devo que feche o valor.
    umDevoQueCubra(devo, aposQuero)
        .ifPresent(
            d -> guardar(opcoes, Opcao.OPCAO_1, juntar(emDiscussao, seria, quero, List.of(d))));

    // Opção 2: todos os seria legal e a combinação de quero com devo de menor intensidade.
    melhorCombinacao(juntar(quero, devo), aposSeria)
        .ifPresent(c -> guardar(opcoes, Opcao.OPCAO_2, juntar(emDiscussao, seria, c)));

    // Opção 3: todos os seria legal e um devo, por valor e intensidade baixa, se cobrir a falta.
    umDevoQueCubra(devo, aposSeria)
        .ifPresent(d -> guardar(opcoes, Opcao.OPCAO_3, juntar(emDiscussao, seria, List.of(d))));

    return List.copyOf(opcoes.values());
  }

  /** Opções com os mesmos itens são a mesma sugestão; fica a primeira. */
  private static void guardar(Map<Set<String>, Sugestao> opcoes, Opcao opcao, List<Item> itens) {
    Set<String> chave = itens.stream().map(Item::nome).collect(Collectors.toSet());
    opcoes.putIfAbsent(chave, Sugestao.de(opcao, itens));
  }

  private static Optional<Item> umDevoQueCubra(List<Item> devo, BigDecimal alvo) {
    return devo.stream()
        .filter(i -> i.valorDeCorte().compareTo(alvo) >= 0)
        .min(
            Comparator.comparing(Item::valorDeCorte)
                .thenComparingInt(Item::intensidade)
                .thenComparing(Item::nome));
  }

  @SafeVarargs
  private static List<Item> juntar(List<Item>... listas) {
    List<Item> todos = new ArrayList<>();
    for (List<Item> lista : listas) {
      todos.addAll(lista);
    }
    return todos;
  }

  private Avaliacao cabeComCortes(
      List<Sugestao> sugestoes, List<Item> listaCompleta, BigDecimal aindaFalta) {
    String motivo =
        aindaFalta == null
            ? Avaliacao.MOTIVO_CABE_COM_CORTES
            : Avaliacao.MOTIVO_CABE_COM_CORTES
                + "; mesmo cortando todos os itens elegíveis ainda faltariam "
                + Dinheiro.formatar(aindaFalta);
    return Avaliacao.cabeComCortes(livre, totalRestante, falta, motivo, sugestoes, listaCompleta);
  }

  /**
   * Entre as combinações do grupo que cobrem o alvo, a de menor intensidade e, depois, a de menor
   * valor. Combinação pode ter quantos itens forem necessários.
   */
  static Optional<List<Item>> melhorCombinacao(List<Item> grupo, BigDecimal alvo) {
    int n = grupo.size();
    if (n > MAXIMO_ITENS_POR_GRUPO) {
      throw new ItensDemaisParaCombinarException(MAXIMO_ITENS_POR_GRUPO);
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
