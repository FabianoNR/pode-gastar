package br.com.fabianonr.podegastar.adaptador.entrada.web;

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
    List<ItemResponse> listaCompleta) {}
