package br.com.fabianonr.podegastar.adaptador.entrada.web;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Avaliação de um item da lista: o eco do item pedido, seguido dos campos da resposta do GET.
 *
 * @param nome como foi enviado, sem cortar espaços
 * @param valor com duas casas
 * @param intensidade 1 quando não foi informada
 */
public record AvaliacaoDoItemResponse(
    String nome,
    String valor,
    String categoria,
    int intensidade,
    @JsonUnwrapped AvaliacaoResponse avaliacao) {}
