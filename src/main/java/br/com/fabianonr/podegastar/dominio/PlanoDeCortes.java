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
      Comparator.comparingInt((Item item) -> posicao(item.categoria()))
          .thenComparingInt(Item::intensidade)
          .thenComparing(Item::id);

  private static final Comparator<List<Item>> COMBINACAO =
      Comparator.<List<Item>>comparingInt(
              combinacao -> combinacao.stream().mapToInt(Item::intensidade).sum())
          .thenComparing(PlanoDeCortes::somaDeCortes)
          .thenComparing(PlanoDeCortes::compararPeloMenorId);

  private final List<Item> itens;
  private final Compra compra;
  private final BigDecimal livre;
  private final BigDecimal totalRestante;
  private final BigDecimal falta;
  private final List<Item> emDiscussao;
  private final List<Item> candidatosSeriaLegal;
  private final List<Item> candidatosQueroFazer;
  private final List<Item> candidatosDevoFazer;

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
            .filter(item -> item.status() == StatusItem.EM_DISCUSSAO && item.elegivelParaCorte())
            .sorted(PRIORIDADE)
            .toList();
    this.candidatosSeriaLegal = candidatos(Categoria.SERIA_LEGAL);
    this.candidatosQueroFazer = candidatos(Categoria.QUERO_FAZER);
    this.candidatosDevoFazer = candidatos(Categoria.DEVO_FAZER);
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
    SelecaoDeCortes selecao = new SelecaoDeCortes(falta);
    cortarItensEmDiscussao(selecao);
    cortarItensPendentesPorGrupo(selecao);
    return decidirResultado(selecao);
  }

  /** Itens em discussão, de qualquer categoria, na ordem de prioridade, até cobrir a falta. */
  private void cortarItensEmDiscussao(SelecaoDeCortes selecao) {
    for (Item item : emDiscussao) {
      if (selecao.cobriuAFalta()) {
        break;
      }
      selecao.adicionar(List.of(item));
    }
  }

  /** Itens pendentes conforme a categoria da compra, grupo a grupo, até cobrir a falta. */
  private void cortarItensPendentesPorGrupo(SelecaoDeCortes selecao) {
    for (List<Item> grupo : gruposDaCompra()) {
      if (selecao.cobriuAFalta()) {
        break;
      }
      List<Item> usados = melhorCombinacao(grupo, selecao.faltaSemCobrir()).orElse(grupo);
      selecao.adicionar(usados);
    }
  }

  private Avaliacao decidirResultado(SelecaoDeCortes selecao) {
    if (selecao.cobriuAFalta()) {
      return cabeComCortes(List.of(Sugestao.de(Opcao.PADRAO, selecao.itens())), List.of(), null);
    }
    return semCobertura(selecao.faltaSemCobrir());
  }

  /** Grupos de PENDENTE (ou PARCIAL) elegíveis, na ordem em que entram na sugestão. */
  private List<List<Item>> gruposDaCompra() {
    List<List<Item>> grupos = new ArrayList<>();
    grupos.add(candidatosSeriaLegal);
    if (compra.categoria() != Categoria.SERIA_LEGAL) {
      grupos.add(candidatosQueroFazer);
    }
    return grupos;
  }

  /** Item que não libera valor ao ser cortado (PARCIAL já gasto além do previsto) fica de fora. */
  private List<Item> candidatos(Categoria categoria) {
    return itens.stream()
        .filter(item -> item.categoria() == categoria)
        .filter(item -> item.status() == StatusItem.PENDENTE || item.status() == StatusItem.PARCIAL)
        .filter(Item::elegivelParaCorte)
        .filter(item -> item.valorDeCorte().signum() > 0)
        .sorted(PRIORIDADE)
        .toList();
  }

  private Avaliacao semCobertura(BigDecimal aindaFalta) {
    if (compra.categoria() == Categoria.SERIA_LEGAL) {
      return Avaliacao.naoCabe(livre, totalRestante, aindaFalta, Motivo.FALTAM_ITENS_SERIA_LEGAL);
    }
    if (compra.categoria() == Categoria.DEVO_FAZER) {
      List<Sugestao> opcoes = opcoesDeUltimoRecurso();
      if (!opcoes.isEmpty()) {
        return cabeComCortes(opcoes, List.of(), null);
      }
    }
    return listaCompletaDeCortes();
  }

  /** Nenhuma combinação cobre a falta: devolve todos os itens elegíveis e quanto ainda faltaria. */
  private Avaliacao listaCompletaDeCortes() {
    List<Item> todos = new ArrayList<>(emDiscussao);
    todos.addAll(candidatosSeriaLegal);
    todos.addAll(candidatosQueroFazer);
    if (compra.categoria() == Categoria.DEVO_FAZER) {
      todos.addAll(candidatosDevoFazer);
    }
    return cabeComCortes(List.of(), todos, falta.subtract(somaDeCortes(todos)));
  }

  /**
   * R7: compra DEVO_FAZER que EM_DISCUSSAO, SERIA_LEGAL e QUERO_FAZER não cobrem. Os itens em
   * discussão entram sempre primeiro, como em R6.
   */
  private List<Sugestao> opcoesDeUltimoRecurso() {
    BigDecimal faltaAposDiscussao = falta.subtract(somaDeCortes(emDiscussao));
    BigDecimal faltaAposSeriaLegal =
        faltaAposDiscussao.subtract(somaDeCortes(candidatosSeriaLegal));
    BigDecimal faltaAposQueroFazer =
        faltaAposSeriaLegal.subtract(somaDeCortes(candidatosQueroFazer));

    Map<Set<String>, Sugestao> opcoes = new LinkedHashMap<>();
    guardarOpcaoUm(opcoes, faltaAposQueroFazer);
    guardarOpcaoDois(opcoes, faltaAposSeriaLegal);
    guardarOpcaoTres(opcoes, faltaAposSeriaLegal);
    return List.copyOf(opcoes.values());
  }

  /** Opção 1: todos os seria legal, todos os quero e um devo que feche o valor. */
  private void guardarOpcaoUm(Map<Set<String>, Sugestao> opcoes, BigDecimal faltaAposQueroFazer) {
    umDevoQueCubra(candidatosDevoFazer, faltaAposQueroFazer)
        .ifPresent(itemDevo ->
            guardar(
                opcoes,
                Opcao.OPCAO_1,
                juntar(
                    emDiscussao, candidatosSeriaLegal, candidatosQueroFazer, List.of(itemDevo))));
  }

  /** Opção 2: todos os seria legal e a combinação de quero com devo de menor intensidade. */
  private void guardarOpcaoDois(Map<Set<String>, Sugestao> opcoes, BigDecimal faltaAposSeriaLegal) {
    melhorCombinacao(juntar(candidatosQueroFazer, candidatosDevoFazer), faltaAposSeriaLegal)
        .ifPresent(combinacao ->
            guardar(opcoes, Opcao.OPCAO_2, juntar(emDiscussao, candidatosSeriaLegal, combinacao)));
  }

  /** Opção 3: todos os seria legal e um devo, por valor e intensidade baixa, se cobrir a falta. */
  private void guardarOpcaoTres(Map<Set<String>, Sugestao> opcoes, BigDecimal faltaAposSeriaLegal) {
    umDevoQueCubra(candidatosDevoFazer, faltaAposSeriaLegal)
        .ifPresent(itemDevo ->
            guardar(
                opcoes,
                Opcao.OPCAO_3,
                juntar(emDiscussao, candidatosSeriaLegal, List.of(itemDevo))));
  }

  /** Opções com os mesmos itens (mesmos ids) são a mesma sugestão; fica a primeira. */
  private static void guardar(Map<Set<String>, Sugestao> opcoes, Opcao opcao, List<Item> itens) {
    Set<String> idsDaOpcao = itens.stream().map(Item::id).collect(Collectors.toSet());
    opcoes.putIfAbsent(idsDaOpcao, Sugestao.de(opcao, itens));
  }

  private static Optional<Item> umDevoQueCubra(List<Item> devo, BigDecimal alvo) {
    return devo.stream()
        .filter(item -> item.valorDeCorte().compareTo(alvo) >= 0)
        .min(
            Comparator.comparing(Item::valorDeCorte)
                .thenComparingInt(Item::intensidade)
                .thenComparing(Item::id));
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
      List<Sugestao> sugestoes, List<Item> listaCompleta, BigDecimal aindaFaltaria) {
    return Avaliacao.cabeComCortes(
        livre, totalRestante, falta, sugestoes, listaCompleta, aindaFaltaria);
  }

  /**
   * Entre as combinações do grupo que cobrem o alvo: menor soma de intensidades, depois menor soma
   * de valores, depois menor id (R6). Combinação pode ter quantos itens forem necessários.
   */
  static Optional<List<Item>> melhorCombinacao(List<Item> grupo, BigDecimal alvo) {
    int quantidadeDeItens = grupo.size();
    if (quantidadeDeItens > MAXIMO_ITENS_POR_GRUPO) {
      throw new ItensDemaisParaCombinarException(MAXIMO_ITENS_POR_GRUPO);
    }
    List<Item> melhor = null;
    for (int mascara = 1; mascara < (1 << quantidadeDeItens); mascara++) {
      List<Item> combinacao = combinacaoDaMascara(grupo, mascara);
      if (somaDeCortes(combinacao).compareTo(alvo) >= 0
          && (melhor == null || COMBINACAO.compare(combinacao, melhor) < 0)) {
        melhor = combinacao;
      }
    }
    return Optional.ofNullable(melhor);
  }

  /** Itens do grupo cujo bit está ligado na máscara. */
  private static List<Item> combinacaoDaMascara(List<Item> grupo, int mascara) {
    List<Item> combinacao = new ArrayList<>();
    for (int i = 0; i < grupo.size(); i++) {
      if ((mascara & (1 << i)) != 0) {
        combinacao.add(grupo.get(i));
      }
    }
    return combinacao;
  }

  /** Compara os ids de cada combinação, em ordem alfabética, até o primeiro diferente. */
  private static int compararPeloMenorId(List<Item> primeira, List<Item> segunda) {
    List<String> idsDaPrimeira = idsEmOrdemAlfabetica(primeira);
    List<String> idsDaSegunda = idsEmOrdemAlfabetica(segunda);
    int quantidadeComum = Math.min(idsDaPrimeira.size(), idsDaSegunda.size());
    for (int i = 0; i < quantidadeComum; i++) {
      int comparacao = idsDaPrimeira.get(i).compareTo(idsDaSegunda.get(i));
      if (comparacao != 0) {
        return comparacao;
      }
    }
    return Integer.compare(idsDaPrimeira.size(), idsDaSegunda.size());
  }

  private static List<String> idsEmOrdemAlfabetica(List<Item> combinacao) {
    return combinacao.stream().map(Item::id).sorted().toList();
  }

  /** Itens já escolhidos para corte e quanto da falta ainda não foi coberto. */
  private static final class SelecaoDeCortes {

    private final List<Item> itens = new ArrayList<>();
    private BigDecimal faltaSemCobrir;

    SelecaoDeCortes(BigDecimal falta) {
      this.faltaSemCobrir = falta;
    }

    void adicionar(List<Item> itensCortados) {
      itens.addAll(itensCortados);
      faltaSemCobrir = faltaSemCobrir.subtract(somaDeCortes(itensCortados));
    }

    boolean cobriuAFalta() {
      return faltaSemCobrir.signum() <= 0;
    }

    BigDecimal faltaSemCobrir() {
      return faltaSemCobrir;
    }

    List<Item> itens() {
      return itens;
    }
  }

  private static int posicao(Categoria categoria) {
    return switch (categoria) {
      case SERIA_LEGAL -> 0;
      case QUERO_FAZER -> 1;
      case DEVO_FAZER -> 2;
    };
  }
}
