package br.com.fabianonr.podegastar.aplicacao.porta.entrada;

import br.com.fabianonr.podegastar.dominio.Compra;

/** Caso de uso: dizer se uma compra cabe no orçamento do mês. */
public interface AvaliarCompra {

  ResultadoDaAvaliacao avaliar(String mesId, Compra compra);
}
