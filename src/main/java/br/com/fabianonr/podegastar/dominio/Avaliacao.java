package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Resultado da avaliação de uma compra, só com dados. Os textos para o usuário são montados pela
 * aplicação.
 *
 * @param aindaFaltaria quanto ainda faltaria mesmo cortando todos os itens elegíveis; só quando não
 *     há combinação que cubra a falta e a resposta traz a lista completa
 * @param itensMaisDesejadosQueACompra itens de {@code sugestoes} e de {@code listaCompleta} com
 *     intensidade maior que a da compra (R12); vazia fora de {@code CABE_COM_CORTES}
 */
public record Avaliacao(
    Decisao decisao,
    BigDecimal livre,
    BigDecimal saldoRestante,
    BigDecimal totalRestante,
    BigDecimal falta,
    Motivo motivo,
    BigDecimal aindaFaltaria,
    List<Sugestao> sugestoes,
    List<Item> listaCompleta,
    List<NomeRepetido> nomesRepetidos,
    List<Item> itensMaisDesejadosQueACompra) {

  static Avaliacao aprovado(BigDecimal livre, BigDecimal totalRestante, BigDecimal saldoRestante) {
    return new Avaliacao(
        Decisao.APROVADO,
        livre,
        saldoRestante,
        totalRestante,
        null,
        null,
        null,
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  static Avaliacao naoCabe(
      BigDecimal livre, BigDecimal totalRestante, BigDecimal falta, Motivo motivo) {
    return new Avaliacao(
        Decisao.NAO_CABE,
        livre,
        null,
        totalRestante,
        falta,
        motivo,
        null,
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  static Avaliacao cabeComCortes(
      BigDecimal livre,
      BigDecimal totalRestante,
      BigDecimal falta,
      List<Sugestao> sugestoes,
      List<Item> listaCompleta,
      BigDecimal aindaFaltaria) {
    return new Avaliacao(
        Decisao.CABE_COM_CORTES,
        livre,
        null,
        totalRestante,
        falta,
        Motivo.CABE_COM_CORTES,
        aindaFaltaria,
        List.copyOf(sugestoes),
        List.copyOf(listaCompleta),
        List.of(),
        List.of());
  }

  /** A mesma avaliação, com os nomes repetidos do mês. */
  Avaliacao comNomesRepetidos(List<NomeRepetido> nomesRepetidosDoMes) {
    return new Avaliacao(
        decisao,
        livre,
        saldoRestante,
        totalRestante,
        falta,
        motivo,
        aindaFaltaria,
        sugestoes,
        listaCompleta,
        List.copyOf(nomesRepetidosDoMes),
        itensMaisDesejadosQueACompra);
  }

  /**
   * A mesma avaliação, com os itens sugeridos ou listados de intensidade estritamente maior que a
   * da compra (R12). Sem repetição pelo id; só na decisão {@code CABE_COM_CORTES}.
   */
  Avaliacao comItensMaisDesejadosQue(Compra compra) {
    if (decisao != Decisao.CABE_COM_CORTES) {
      return this;
    }
    Map<String, Item> maisDesejadosPorId = new LinkedHashMap<>();
    Stream.concat(
            sugestoes.stream().flatMap(sugestao -> sugestao.itens().stream()),
            listaCompleta.stream())
        .filter(item -> item.intensidade() > compra.intensidade())
        .forEach(item -> maisDesejadosPorId.putIfAbsent(item.id(), item));
    return new Avaliacao(
        decisao,
        livre,
        saldoRestante,
        totalRestante,
        falta,
        motivo,
        aindaFaltaria,
        sugestoes,
        listaCompleta,
        nomesRepetidos,
        List.copyOf(maisDesejadosPorId.values()));
  }
}
