package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Monta as sugestões de corte (R5 a R7) para uma compra que não cabe no livre, mas cabe no total
 * restante.
 */
final class PlanoDeCortes {

  /**
   * Limite de itens de um grupo na busca de combinações (2^n). Nenhum grupo acima dele chega à
   * busca (ADR 0012).
   */
  private static final int MAXIMO_ITENS_POR_GRUPO = 20;

  /**
   * Ordem dos itens dentro do grupo e na sugestão. A categoria só distingue itens no grupo dos
   * itens em discussão: seria legal, depois quero, depois devo; depois a intensidade e o id.
   */
  private static final Comparator<Item> ORDEM_NA_SUGESTAO =
      Comparator.comparingInt((Item item) -> posicao(item.categoria()))
          .thenComparingInt(Item::intensidade)
          .thenComparing(Item::id);

  /** Regra de corte (R6): menor soma de cortáveis, depois menor soma de intensidades, depois id. */
  private static final Comparator<List<Item>> MENOR_CORTE =
      Comparator.comparing(PlanoDeCortes::somaDeCortes)
          .thenComparingInt(PlanoDeCortes::somaDeIntensidades)
          .thenComparing(PlanoDeCortes::compararPeloMenorId);

  /** Opção 2 (R7): menor soma de intensidades, depois menor soma de cortáveis, depois id. */
  private static final Comparator<List<Item>> MENOR_INTENSIDADE =
      Comparator.comparingInt(PlanoDeCortes::somaDeIntensidades)
          .thenComparing(PlanoDeCortes::somaDeCortes)
          .thenComparing(PlanoDeCortes::compararPeloMenorId);

  /** Opções 1 e 3 (R7): menor intensidade, depois menor cortável, depois menor id. */
  private static final Comparator<Item> DEVO_FAZER_PREFERIDO =
      Comparator.comparingInt(Item::intensidade)
          .thenComparing(Item::valorDeCorte)
          .thenComparing(Item::id);

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
    this.compra = compra;
    this.livre = livre;
    this.totalRestante = totalRestante;
    this.falta = falta;
    this.emDiscussao =
        itens.stream()
            .filter(item -> item.status() == StatusItem.EM_DISCUSSAO && item.elegivelParaCorte())
            .sorted(ORDEM_NA_SUGESTAO)
            .toList();
    this.candidatosSeriaLegal = candidatos(itens, Categoria.SERIA_LEGAL);
    this.candidatosQueroFazer = candidatos(itens, Categoria.QUERO_FAZER);
    this.candidatosDevoFazer = candidatos(itens, Categoria.DEVO_FAZER);
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
    exigirLimiteDosGruposConferidosAntes();
    List<List<Item>> grupos = gruposDaCompra();
    return regraDeCorte(grupos)
        .map(itensCortados ->
            cabeComCortes(List.of(Sugestao.de(Opcao.PADRAO, itensCortados)), List.of(), null))
        .orElseGet(() -> semCobertura(grupos));
  }

  /**
   * Itens em discussão e o DEVO_FAZER da compra QUERO_FAZER são conferidos antes de qualquer
   * combinação, mesmo que o grupo não seja usado (ADR 0012).
   */
  private void exigirLimiteDosGruposConferidosAntes() {
    exigirLimiteDeItens(emDiscussao);
    if (compra.categoria() == Categoria.QUERO_FAZER) {
      exigirLimiteDeItens(candidatosDevoFazer);
    }
  }

  /** Grupos de corte da compra, do menos importante para o mais importante (R6). */
  private List<List<Item>> gruposDaCompra() {
    return switch (compra.categoria()) {
      case SERIA_LEGAL -> List.of(emDiscussao, candidatosSeriaLegal);
      case QUERO_FAZER ->
          List.of(emDiscussao, candidatosSeriaLegal, candidatosQueroFazer, candidatosDevoFazer);
      case DEVO_FAZER -> List.of(emDiscussao, candidatosSeriaLegal, candidatosQueroFazer);
    };
  }

  /**
   * Regra de corte (R6): corta primeiro o menos importante; do mais importante, só o mínimo que
   * faltar. Vazio quando nem todos os grupos juntos cobrem a falta.
   */
  private Optional<List<Item>> regraDeCorte(List<List<Item>> grupos) {
    if (somaDeCortes(juntar(grupos)).compareTo(falta) < 0) {
      return Optional.empty();
    }
    List<List<Item>> cortesPorGrupo =
        new ArrayList<>(Collections.nCopies(grupos.size(), List.of()));
    BigDecimal faltaQueResta = falta;
    for (int posicaoDoGrupo = grupos.size() - 1; posicaoDoGrupo >= 0; posicaoDoGrupo--) {
      BigDecimal cobertoPelosMenosImportantes =
          somaDeCortes(juntar(grupos.subList(0, posicaoDoGrupo)));
      BigDecimal necessarioDoGrupo = faltaQueResta.subtract(cobertoPelosMenosImportantes);
      if (necessarioDoGrupo.signum() > 0) {
        List<Item> cortesDoGrupo =
            melhorCombinacao(grupos.get(posicaoDoGrupo), necessarioDoGrupo, MENOR_CORTE)
                .orElseThrow();
        cortesPorGrupo.set(posicaoDoGrupo, cortesDoGrupo);
        faltaQueResta = faltaQueResta.subtract(somaDeCortes(cortesDoGrupo));
      }
    }
    return Optional.of(juntar(cortesPorGrupo));
  }

  /** Item que não libera valor ao ser cortado (PARCIAL já gasto além do previsto) fica de fora. */
  private static List<Item> candidatos(List<Item> itens, Categoria categoria) {
    return itens.stream()
        .filter(item -> item.categoria() == categoria)
        .filter(item -> item.status() == StatusItem.PENDENTE || item.status() == StatusItem.PARCIAL)
        .filter(Item::elegivelParaCorte)
        .filter(item -> item.valorDeCorte().signum() > 0)
        .sorted(ORDEM_NA_SUGESTAO)
        .toList();
  }

  private Avaliacao semCobertura(List<List<Item>> grupos) {
    if (compra.categoria() == Categoria.SERIA_LEGAL) {
      BigDecimal aindaFaltaria = falta.subtract(somaDeCortes(juntar(grupos)));
      return Avaliacao.naoCabe(
          livre, totalRestante, aindaFaltaria, Motivo.FALTAM_ITENS_SERIA_LEGAL);
    }
    if (compra.categoria() == Categoria.DEVO_FAZER) {
      List<Sugestao> opcoes = opcoesDeUltimoRecurso();
      if (!opcoes.isEmpty()) {
        return cabeComCortes(opcoes, List.of(), null);
      }
    }
    return listaCompletaDeCortes();
  }

  /** Nenhum corte cobre a falta: devolve todos os itens elegíveis e quanto ainda faltaria (R7). */
  private Avaliacao listaCompletaDeCortes() {
    List<Item> todos =
        juntar(
            List.of(emDiscussao, candidatosSeriaLegal, candidatosQueroFazer, candidatosDevoFazer));
    return cabeComCortes(List.of(), todos, falta.subtract(somaDeCortes(todos)));
  }

  /**
   * R7: compra DEVO_FAZER que a regra de corte não cobre. Todas as opções levam os itens em
   * discussão e os seria legal elegíveis.
   */
  private List<Sugestao> opcoesDeUltimoRecurso() {
    BigDecimal faltaAposSeriaLegal =
        falta.subtract(somaDeCortes(emDiscussao)).subtract(somaDeCortes(candidatosSeriaLegal));
    BigDecimal faltaAposQueroFazer =
        faltaAposSeriaLegal.subtract(somaDeCortes(candidatosQueroFazer));

    Map<Set<String>, Sugestao> opcoes = new LinkedHashMap<>();
    guardarOpcaoUm(opcoes, faltaAposQueroFazer);
    guardarOpcaoDois(opcoes, faltaAposSeriaLegal);
    guardarOpcaoTres(opcoes, faltaAposSeriaLegal);
    return List.copyOf(opcoes.values());
  }

  /** Opção 1: todos os quero e um devo que, sozinho, cobre o que ainda falta. */
  private void guardarOpcaoUm(Map<Set<String>, Sugestao> opcoes, BigDecimal faltaAposQueroFazer) {
    umDevoQueCubra(faltaAposQueroFazer)
        .ifPresent(itemDevo ->
            guardar(
                opcoes,
                Opcao.OPCAO_1,
                juntar(
                    List.of(
                        emDiscussao,
                        candidatosSeriaLegal,
                        candidatosQueroFazer,
                        List.of(itemDevo)))));
  }

  /** Opção 2: a combinação de quero com devo de menor soma de intensidades. */
  private void guardarOpcaoDois(Map<Set<String>, Sugestao> opcoes, BigDecimal faltaAposSeriaLegal) {
    List<Item> queroEDevo = juntar(List.of(candidatosQueroFazer, candidatosDevoFazer));
    melhorCombinacao(queroEDevo, faltaAposSeriaLegal, MENOR_INTENSIDADE)
        .ifPresent(combinacao ->
            guardar(
                opcoes,
                Opcao.OPCAO_2,
                juntar(List.of(emDiscussao, candidatosSeriaLegal, combinacao))));
  }

  /** Opção 3: um devo que, sozinho, cobre o que falta depois dos seria legal. */
  private void guardarOpcaoTres(Map<Set<String>, Sugestao> opcoes, BigDecimal faltaAposSeriaLegal) {
    umDevoQueCubra(faltaAposSeriaLegal)
        .ifPresent(itemDevo ->
            guardar(
                opcoes,
                Opcao.OPCAO_3,
                juntar(List.of(emDiscussao, candidatosSeriaLegal, List.of(itemDevo)))));
  }

  /** Opções com os mesmos itens (mesmos ids) são a mesma sugestão; fica a primeira. */
  private static void guardar(Map<Set<String>, Sugestao> opcoes, Opcao opcao, List<Item> itens) {
    Set<String> idsDaOpcao = itens.stream().map(Item::id).collect(Collectors.toSet());
    opcoes.putIfAbsent(idsDaOpcao, Sugestao.de(opcao, itens));
  }

  private Optional<Item> umDevoQueCubra(BigDecimal alvo) {
    return candidatosDevoFazer.stream()
        .filter(item -> item.valorDeCorte().compareTo(alvo) >= 0)
        .min(DEVO_FAZER_PREFERIDO);
  }

  private static List<Item> juntar(List<List<Item>> listas) {
    List<Item> todos = new ArrayList<>();
    listas.forEach(todos::addAll);
    return todos;
  }

  private Avaliacao cabeComCortes(
      List<Sugestao> sugestoes, List<Item> listaCompleta, BigDecimal aindaFaltaria) {
    return Avaliacao.cabeComCortes(
        livre, totalRestante, falta, sugestoes, listaCompleta, aindaFaltaria);
  }

  private static void exigirLimiteDeItens(List<Item> itensCombinados) {
    if (itensCombinados.size() > MAXIMO_ITENS_POR_GRUPO) {
      throw new ItensDemaisParaCombinarException(MAXIMO_ITENS_POR_GRUPO);
    }
  }

  /**
   * Melhor combinação de itens que cobre o alvo, pelo critério informado. Uma combinação pode ter
   * quantos itens forem necessários e mantém a ordem dos itens no grupo. A busca testa todas as
   * combinações (2^n); por isso recusa grupos acima do limite antes de começar.
   */
  private static Optional<List<Item>> melhorCombinacao(
      List<Item> grupo, BigDecimal alvo, Comparator<List<Item>> criterio) {
    exigirLimiteDeItens(grupo);
    int quantidadeDeItens = grupo.size();
    List<Item> melhor = null;
    for (int mascara = 1; mascara < (1 << quantidadeDeItens); mascara++) {
      List<Item> combinacao = combinacaoDaMascara(grupo, mascara);
      if (somaDeCortes(combinacao).compareTo(alvo) >= 0
          && (melhor == null || criterio.compare(combinacao, melhor) < 0)) {
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

  private static int somaDeIntensidades(List<Item> combinacao) {
    return combinacao.stream().mapToInt(Item::intensidade).sum();
  }

  /**
   * Compara os ids de cada combinação, em ordem numérica natural (item-2 antes de item-10), até o
   * primeiro diferente (ADR 0011).
   */
  private static int compararPeloMenorId(List<Item> primeira, List<Item> segunda) {
    List<String> idsDaPrimeira = idsEmOrdemNatural(primeira);
    List<String> idsDaSegunda = idsEmOrdemNatural(segunda);
    int quantidadeComum = Math.min(idsDaPrimeira.size(), idsDaSegunda.size());
    for (int i = 0; i < quantidadeComum; i++) {
      int comparacao = OrdemNaturalDeIds.compararIds(idsDaPrimeira.get(i), idsDaSegunda.get(i));
      if (comparacao != 0) {
        return comparacao;
      }
    }
    return Integer.compare(idsDaPrimeira.size(), idsDaSegunda.size());
  }

  private static List<String> idsEmOrdemNatural(List<Item> combinacao) {
    return combinacao.stream().map(Item::id).sorted(OrdemNaturalDeIds::compararIds).toList();
  }

  private static int posicao(Categoria categoria) {
    return switch (categoria) {
      case SERIA_LEGAL -> 0;
      case QUERO_FAZER -> 1;
      case DEVO_FAZER -> 2;
    };
  }
}
