package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompras;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de vários itens (ADR 0018). Nada é criado nem alterado. O corpo chega como texto para
 * ser lido e validado à mão, na ordem do contrato, antes de procurar o mês.
 */
@RestController
public class AvaliacoesController {

  private final AvaliarCompras avaliarCompras;

  public AvaliacoesController(AvaliarCompras avaliarCompras) {
    this.avaliarCompras = avaliarCompras;
  }

  @PostMapping("/meses/{mesId}/avaliacoes")
  public AvaliacoesResponse avaliar(
      @PathVariable String mesId, @RequestBody(required = false) String corpo) {
    return AvaliacoesMapper.paraResposta(
        avaliarCompras.avaliar(mesId, LeitorDoPedidoDeCompras.ler(corpo)));
  }
}
