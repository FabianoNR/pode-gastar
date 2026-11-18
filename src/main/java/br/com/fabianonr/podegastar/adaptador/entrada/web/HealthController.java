package br.com.fabianonr.podegastar.adaptador.entrada.web;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe o status e o commit no ar (spec, O1 a O3). */
@RestController
public class HealthController {

  private static final String STATUS_NO_AR = "UP";
  private static final String VARIAVEL_DO_COMMIT = "RENDER_GIT_COMMIT";
  private static final String COMMIT_DESCONHECIDO = "desconhecido";

  private final Environment ambiente;

  public HealthController(Environment ambiente) {
    this.ambiente = ambiente;
  }

  @GetMapping("/health")
  public HealthResponse health() {
    return new HealthResponse(STATUS_NO_AR, commitNoAr());
  }

  private String commitNoAr() {
    var valorDaVariavel = ambiente.getProperty(VARIAVEL_DO_COMMIT, "").strip();
    return valorDaVariavel.isEmpty() ? COMMIT_DESCONHECIDO : valorDaVariavel;
  }
}
