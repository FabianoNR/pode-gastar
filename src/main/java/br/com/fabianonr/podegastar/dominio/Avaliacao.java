package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Avaliacao(
    Decisao decisao,
    BigDecimal livre,
    BigDecimal saldoRestante,
    BigDecimal totalRestante,
    BigDecimal falta,
    String motivo,
    List<Sugestao> sugestoes,
    List<Item> listaCompleta) {

  public static final String MOTIVO_NAO_CABE = "compra excede o total que ainda sobra no mês";
  public static final String MOTIVO_CABE_COM_CORTES =
      "compra não cabe no livre; cabe no total restante com cortes";

  static Avaliacao aprovado(BigDecimal livre, BigDecimal totalRestante, BigDecimal saldoRestante) {
    return new Avaliacao(
        Decisao.APROVADO, livre, saldoRestante, totalRestante, null, null, List.of(), List.of());
  }

  static Avaliacao naoCabe(
      BigDecimal livre, BigDecimal totalRestante, BigDecimal falta, String motivo) {
    return new Avaliacao(
        Decisao.NAO_CABE, livre, null, totalRestante, falta, motivo, List.of(), List.of());
  }

  static Avaliacao cabeComCortes(
      BigDecimal livre,
      BigDecimal totalRestante,
      BigDecimal falta,
      String motivo,
      List<Sugestao> sugestoes,
      List<Item> listaCompleta) {
    return new Avaliacao(
        Decisao.CABE_COM_CORTES,
        livre,
        null,
        totalRestante,
        falta,
        motivo,
        List.copyOf(sugestoes),
        List.copyOf(listaCompleta));
  }
}
