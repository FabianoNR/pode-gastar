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
    List<Item> listaCompleta) {}
