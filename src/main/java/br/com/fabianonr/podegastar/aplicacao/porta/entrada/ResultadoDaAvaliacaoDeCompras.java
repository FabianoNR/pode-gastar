package br.com.fabianonr.podegastar.aplicacao.porta.entrada;

import java.util.List;

/**
 * Resultado da consulta de várias compras.
 *
 * @param avisosDoMes um texto por nome repetido no mês, uma vez só para a lista toda
 * @param comprasAvaliadas maior intensidade primeiro; no empate, a ordem do pedido
 */
public record ResultadoDaAvaliacaoDeCompras(
    List<String> avisosDoMes, List<CompraAvaliada> comprasAvaliadas) {

  public ResultadoDaAvaliacaoDeCompras {
    avisosDoMes = List.copyOf(avisosDoMes);
    comprasAvaliadas = List.copyOf(comprasAvaliadas);
  }
}
