package br.com.fabianonr.podegastar.adaptador.entrada.web;

import java.util.List;

public record SugestaoResponse(String opcao, List<ItemResponse> itens, String cobre) {}
