package br.com.fabianonr.podegastar.adaptador.entrada.web;

import java.util.List;

/**
 * Corpo da resposta 200 da consulta de vários itens.
 *
 * @param avisos os avisos de nome repetido do mês, uma vez só para a lista toda
 */
public record AvaliacoesResponse(List<String> avisos, List<AvaliacaoDoItemResponse> avaliacoes) {}
