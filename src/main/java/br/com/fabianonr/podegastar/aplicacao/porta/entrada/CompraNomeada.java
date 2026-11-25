package br.com.fabianonr.podegastar.aplicacao.porta.entrada;

import br.com.fabianonr.podegastar.dominio.Compra;

/**
 * Compra de uma lista, com o nome que só a identifica na resposta e não muda o resultado.
 *
 * @param nome como quem consultou enviou, sem cortar espaços
 */
public record CompraNomeada(String nome, Compra compra) {}
