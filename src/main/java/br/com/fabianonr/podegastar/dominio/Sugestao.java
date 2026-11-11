package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Sugestao(List<Item> itens, BigDecimal cobre) {}
