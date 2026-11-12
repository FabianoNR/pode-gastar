package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.dominio.Decisao;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Corpo da resposta 200. Campos que não se aplicam à decisão ficam fora do JSON. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvaliacaoResponse(
    String decisao,
    String totalRestante,
    String livre,
    String saldoRestante,
    String falta,
    String motivo,
    List<SugestaoResponse> sugestoes,
    List<ItemResponse> listaCompleta,
    List<String> avisos) {

  static AvaliacaoResponse aprovada(String livre, String saldoRestante, List<String> avisos) {
    return new AvaliacaoResponse(
        Decisao.APROVADO.name(), null, livre, saldoRestante, null, null, null, null, avisos);
  }

  static AvaliacaoResponse cabeComCortes(
      String totalRestante,
      String livre,
      String falta,
      String motivo,
      List<SugestaoResponse> sugestoes,
      List<ItemResponse> listaCompleta,
      List<String> avisos) {
    return new AvaliacaoResponse(
        Decisao.CABE_COM_CORTES.name(),
        totalRestante,
        livre,
        null,
        falta,
        motivo,
        sugestoes,
        listaCompleta,
        avisos);
  }

  static AvaliacaoResponse naoCabe(
      String totalRestante, String falta, String motivo, List<String> avisos) {
    return new AvaliacaoResponse(
        Decisao.NAO_CABE.name(), totalRestante, null, null, falta, motivo, null, null, avisos);
  }
}
