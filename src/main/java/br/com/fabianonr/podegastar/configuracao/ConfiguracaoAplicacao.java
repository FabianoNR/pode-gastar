package br.com.fabianonr.podegastar.configuracao;

import br.com.fabianonr.podegastar.adaptador.saida.memoria.MesEmMemoriaRepositorio;
import br.com.fabianonr.podegastar.aplicacao.AvaliarCompraService;
import br.com.fabianonr.podegastar.aplicacao.AvaliarComprasService;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompra;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompras;
import br.com.fabianonr.podegastar.aplicacao.porta.saida.MesRepositorio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Liga os casos de uso às portas de saída. É o único lugar que conhece os adaptadores de saída. */
@Configuration
public class ConfiguracaoAplicacao {

  @Bean
  MesRepositorio mesRepositorio() {
    return new MesEmMemoriaRepositorio();
  }

  @Bean
  AvaliarCompra avaliarCompra(MesRepositorio meses) {
    return new AvaliarCompraService(meses);
  }

  @Bean
  AvaliarCompras avaliarCompras(MesRepositorio meses) {
    return new AvaliarComprasService(meses);
  }
}
