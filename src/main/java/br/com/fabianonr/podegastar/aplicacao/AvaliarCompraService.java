package br.com.fabianonr.podegastar.aplicacao;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompra;
import br.com.fabianonr.podegastar.aplicacao.porta.saida.MesRepositorio;
import br.com.fabianonr.podegastar.dominio.Avaliacao;
import br.com.fabianonr.podegastar.dominio.Compra;

public class AvaliarCompraService implements AvaliarCompra {

  private final MesRepositorio meses;

  public AvaliarCompraService(MesRepositorio meses) {
    this.meses = meses;
  }

  @Override
  public Avaliacao avaliar(String mesId, Compra compra) {
    return meses
        .buscarPorId(mesId)
        .orElseThrow(() -> new MesNaoEncontradoException(mesId))
        .avaliar(compra);
  }
}
