package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.dominio.Avaliacao;
import br.com.fabianonr.podegastar.dominio.Decisao;
import br.com.fabianonr.podegastar.dominio.Item;
import br.com.fabianonr.podegastar.dominio.Sugestao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Traduz o domínio para o JSON do contrato (ADR 0003). */
final class AvaliacaoMapper {

  private AvaliacaoMapper() {}

  static AvaliacaoResponse paraResposta(Avaliacao a) {
    return switch (a.decisao()) {
      case APROVADO ->
          new AvaliacaoResponse(
              nome(a.decisao()),
              null,
              dinheiro(a.livre()),
              dinheiro(a.saldoRestante()),
              null,
              null,
              null,
              null);
      case CABE_COM_CORTES ->
          new AvaliacaoResponse(
              nome(a.decisao()),
              dinheiro(a.totalRestante()),
              dinheiro(a.livre()),
              null,
              dinheiro(a.falta()),
              a.motivo(),
              a.sugestoes().stream().map(AvaliacaoMapper::sugestao).toList(),
              a.listaCompleta().stream().map(AvaliacaoMapper::item).toList());
      case NAO_CABE ->
          new AvaliacaoResponse(
              nome(a.decisao()),
              dinheiro(a.totalRestante()),
              null,
              null,
              dinheiro(a.falta()),
              a.motivo(),
              null,
              null);
    };
  }

  private static SugestaoResponse sugestao(Sugestao s) {
    List<ItemResponse> itens = s.itens().stream().map(AvaliacaoMapper::item).toList();
    return new SugestaoResponse(s.opcao().name(), itens, dinheiro(s.cobre()));
  }

  private static ItemResponse item(Item i) {
    return new ItemResponse(i.nome(), i.status().name(), i.decisorResponsavel());
  }

  private static String nome(Decisao decisao) {
    return decisao.name();
  }

  private static String dinheiro(BigDecimal valor) {
    return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }
}
