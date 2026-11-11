package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;

public record Item(
    String nome,
    Categoria categoria,
    int intensidade,
    boolean planejado,
    StatusItem status,
    BigDecimal valorEstimado,
    BigDecimal valorRealizado,
    String decisorResponsavel) {}
