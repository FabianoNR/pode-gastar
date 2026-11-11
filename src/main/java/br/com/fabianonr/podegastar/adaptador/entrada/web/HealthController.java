package br.com.fabianonr.podegastar.adaptador.entrada.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/health")
  public Map<String, String> health() {
    throw new UnsupportedOperationException("não implementado");
  }
}
