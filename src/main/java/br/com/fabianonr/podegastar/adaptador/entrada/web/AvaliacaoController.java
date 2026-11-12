package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompra;
import br.com.fabianonr.podegastar.dominio.Categoria;
import br.com.fabianonr.podegastar.dominio.Compra;
import br.com.fabianonr.podegastar.dominio.ValorInvalidoException;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvaliacaoController {

  private static final int CASAS_DECIMAIS = 2;

  private final AvaliarCompra avaliarCompra;

  public AvaliacaoController(AvaliarCompra avaliarCompra) {
    this.avaliarCompra = avaliarCompra;
  }

  @GetMapping("/meses/{mesId}/avaliacao")
  public AvaliacaoResponse avaliar(
      @PathVariable String mesId,
      @RequestParam BigDecimal valor,
      @RequestParam Categoria categoria) {
    if (valor.stripTrailingZeros().scale() > CASAS_DECIMAIS) {
      throw new ValorInvalidoException("valor deve ter no máximo duas casas decimais");
    }
    return AvaliacaoMapper.paraResposta(avaliarCompra.avaliar(mesId, new Compra(valor, categoria)));
  }
}
