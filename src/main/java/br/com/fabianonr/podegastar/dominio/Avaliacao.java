package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado da avaliação de uma compra, só com dados. Os textos para o usuário são montados pela
 * aplicação.
 *
 * @param aindaFaltaria quanto ainda faltaria mesmo cortando todos os itens elegíveis; só quando não
 *     há combinação que cubra a falta e a resposta traz a lista completa
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
    List<NomeRepetido> nomesRepetidos) {

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
        List.copyOf(nomesRepetidosDoMes));
  }
}
