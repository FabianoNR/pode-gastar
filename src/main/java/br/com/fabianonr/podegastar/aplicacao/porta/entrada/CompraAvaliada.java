package br.com.fabianonr.podegastar.aplicacao.porta.entrada;

/**
 * Uma compra da lista com a avaliação dela. Os avisos do resultado trazem só o aviso de intensidade
 * (R12); os avisos de nome repetido do mês ficam no resultado da lista.
 */
public record CompraAvaliada(CompraNomeada compraNomeada, ResultadoDaAvaliacao resultado) {}
