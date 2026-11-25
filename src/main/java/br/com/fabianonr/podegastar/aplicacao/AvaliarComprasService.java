package br.com.fabianonr.podegastar.aplicacao;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompras;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.CompraNomeada;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.ResultadoDaAvaliacaoDeCompras;
import br.com.fabianonr.podegastar.aplicacao.porta.saida.MesRepositorio;
import java.util.List;

public class AvaliarComprasService implements AvaliarCompras {

  private final MesRepositorio meses;

  public AvaliarComprasService(MesRepositorio meses) {
    this.meses = meses;
  }

  @Override
  public ResultadoDaAvaliacaoDeCompras avaliar(String mesId, List<CompraNomeada> compras) {
    return new ResultadoDaAvaliacaoDeCompras(List.of(), List.of());
  }
}
