package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompra;
import br.com.fabianonr.podegastar.dominio.Categoria;
import br.com.fabianonr.podegastar.dominio.Compra;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvaliacaoController {

  private final AvaliarCompra avaliarCompra;

  public AvaliacaoController(AvaliarCompra avaliarCompra) {
    this.avaliarCompra = avaliarCompra;
  }

  @GetMapping("/meses/{mesId}/avaliacao")
  public AvaliacaoResponse avaliar(
      @PathVariable String mesId,
      @RequestParam BigDecimal valor,
      @RequestParam Categoria categoria,
      @RequestParam(required = false) Integer intensidade) {
    CasasDecimaisDoValor.exigirNoMaximoDuas(valor, "valor");
    return AvaliacaoMapper.paraResposta(
        avaliarCompra.avaliar(mesId, compraDe(valor, categoria, intensidade)));
  }

  private static Compra compraDe(BigDecimal valor, Categoria categoria, Integer intensidade) {
    if (intensidade == null) {
      return new Compra(valor, categoria);
    }
    return new Compra(valor, categoria, intensidade);
  }
}
