package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.CompraAvaliada;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.ResultadoDaAvaliacaoDeCompras;
import br.com.fabianonr.podegastar.dominio.Compra;
import br.com.fabianonr.podegastar.dominio.Dinheiro;

/** Traduz o resultado da consulta de vários itens para o JSON do contrato. Não monta textos. */
final class AvaliacoesMapper {

  private AvaliacoesMapper() {}

  static AvaliacoesResponse paraResposta(ResultadoDaAvaliacaoDeCompras resultado) {
    return new AvaliacoesResponse(
        resultado.avisosDoMes(),
        resultado.comprasAvaliadas().stream().map(AvaliacoesMapper::avaliacaoDoItem).toList());
  }

  private static AvaliacaoDoItemResponse avaliacaoDoItem(CompraAvaliada compraAvaliada) {
    Compra compra = compraAvaliada.compraNomeada().compra();
    return new AvaliacaoDoItemResponse(
        compraAvaliada.compraNomeada().nome(),
        Dinheiro.formatar(compra.valor()),
        compra.categoria().name(),
        compra.intensidade(),
        AvaliacaoMapper.paraResposta(compraAvaliada.resultado()));
  }
}
