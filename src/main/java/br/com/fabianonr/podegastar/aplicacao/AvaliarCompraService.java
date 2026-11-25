package br.com.fabianonr.podegastar.aplicacao;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompra;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.ResultadoDaAvaliacao;
import br.com.fabianonr.podegastar.aplicacao.porta.saida.MesRepositorio;
import br.com.fabianonr.podegastar.dominio.Avaliacao;
import br.com.fabianonr.podegastar.dominio.Compra;
import java.util.ArrayList;
import java.util.List;

public class AvaliarCompraService implements AvaliarCompra {

  private final MesRepositorio meses;

  public AvaliarCompraService(MesRepositorio meses) {
    this.meses = meses;
  }

  @Override
  public ResultadoDaAvaliacao avaliar(String mesId, Compra compra) {
    Avaliacao avaliacao =
        meses
            .buscarPorId(mesId)
            .orElseThrow(() -> new MesNaoEncontradoException(mesId))
            .avaliar(compra);
    return MensagensDaAvaliacao.resultado(avaliacao, avisos(avaliacao, compra));
  }

  /** Um aviso por nome repetido e, depois, o aviso de intensidade, quando há (R12). */
  private static List<String> avisos(Avaliacao avaliacao, Compra compra) {
    List<String> avisos = new ArrayList<>();
    avaliacao.nomesRepetidos().stream().map(MensagensDaAvaliacao::aviso).forEach(avisos::add);
    String avisoDeIntensidade = MensagensDaAvaliacao.avisoDeIntensidade(avaliacao, compra);
    if (avisoDeIntensidade != null) {
      avisos.add(avisoDeIntensidade);
    }
    return avisos;
  }
}
