package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.ResultadoDaAvaliacao;
import br.com.fabianonr.podegastar.dominio.Avaliacao;
import br.com.fabianonr.podegastar.dominio.Dinheiro;
import br.com.fabianonr.podegastar.dominio.Item;
import br.com.fabianonr.podegastar.dominio.Sugestao;
import java.util.List;

/** Traduz o resultado da aplicação para o JSON do contrato (ADR 0003). Não monta textos. */
final class AvaliacaoMapper {

  private AvaliacaoMapper() {}

  static AvaliacaoResponse paraResposta(ResultadoDaAvaliacao resultado) {
    Avaliacao avaliacao = resultado.avaliacao();
    String decisao = avaliacao.decisao().name();
    return switch (avaliacao.decisao()) {
      case APROVADO ->
          new AvaliacaoResponse(
              decisao,
              null,
              Dinheiro.formatar(avaliacao.livre()),
              Dinheiro.formatar(avaliacao.saldoRestante()),
              null,
              null,
              null,
              null,
              resultado.avisos());
      case CABE_COM_CORTES ->
          new AvaliacaoResponse(
              decisao,
              Dinheiro.formatar(avaliacao.totalRestante()),
              Dinheiro.formatar(avaliacao.livre()),
              null,
              Dinheiro.formatar(avaliacao.falta()),
              resultado.motivo(),
              avaliacao.sugestoes().stream()
                  .map(sugestao -> sugestao(sugestao, resultado))
                  .toList(),
              avaliacao.listaCompleta().stream().map(item -> item(item, resultado)).toList(),
              resultado.avisos());
      case NAO_CABE ->
          new AvaliacaoResponse(
              decisao,
              Dinheiro.formatar(avaliacao.totalRestante()),
              null,
              null,
              Dinheiro.formatar(avaliacao.falta()),
              resultado.motivo(),
              null,
              null,
              resultado.avisos());
    };
  }

  private static SugestaoResponse sugestao(Sugestao sugestao, ResultadoDaAvaliacao resultado) {
    List<ItemResponse> itens =
        sugestao.itens().stream().map(item -> item(item, resultado)).toList();
    return new SugestaoResponse(
        sugestao.opcao().name(), itens, Dinheiro.formatar(sugestao.cobre()));
  }

  private static ItemResponse item(Item item, ResultadoDaAvaliacao resultado) {
    return new ItemResponse(
        item.id(),
        item.nome(),
        item.status().name(),
        item.decisorResponsavel(),
        resultado.mensagemDoItem(item.id()));
  }
}
