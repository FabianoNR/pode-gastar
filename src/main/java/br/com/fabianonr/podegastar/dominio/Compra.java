package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;

public record Compra(BigDecimal valor, Categoria categoria) {}
