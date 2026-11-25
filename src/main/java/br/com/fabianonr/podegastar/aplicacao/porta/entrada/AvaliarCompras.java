package br.com.fabianonr.podegastar.aplicacao.porta.entrada;

import java.util.List;

/**
 * Caso de uso: dizer, para cada compra de uma lista, se ela cabe no orçamento do mês. Cada compra é
 * avaliada sozinha contra o mesmo estado do mês.
 */
public interface AvaliarCompras {

  ResultadoDaAvaliacaoDeCompras avaliar(String mesId, List<CompraNomeada> compras);
}
