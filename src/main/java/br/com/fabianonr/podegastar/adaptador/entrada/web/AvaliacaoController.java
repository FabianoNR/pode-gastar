package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.dominio.Categoria;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvaliacaoController {

  @GetMapping("/meses/{mesId}/avaliacao")
  public Object avaliar(
      @PathVariable String mesId,
      @RequestParam BigDecimal valor,
      @RequestParam Categoria categoria) {
    throw new UnsupportedOperationException("não implementado");
  }
}
