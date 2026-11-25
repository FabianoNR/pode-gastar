package br.com.fabianonr.podegastar.aplicacao;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompras;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.CompraAvaliada;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.CompraNomeada;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.ResultadoDaAvaliacaoDeCompras;
import br.com.fabianonr.podegastar.aplicacao.porta.saida.MesRepositorio;
import br.com.fabianonr.podegastar.dominio.Avaliacao;
import br.com.fabianonr.podegastar.dominio.Compra;
import br.com.fabianonr.podegastar.dominio.Mes;
import java.util.Comparator;
import java.util.List;

/**
 * Avalia cada compra da lista sozinha, contra o mesmo mês, lido uma vez. Os avisos de nome repetido
 * saem uma vez para a lista; cada avaliação leva só o próprio aviso de intensidade (R12).
 */
public class AvaliarComprasService implements AvaliarCompras {

  private static final Comparator<CompraAvaliada> MAIOR_INTENSIDADE_PRIMEIRO =
      Comparator.comparingInt((CompraAvaliada compraAvaliada) ->
              compraAvaliada.compraNomeada().compra().intensidade())
          .reversed();

  private final MesRepositorio meses;

  public AvaliarComprasService(MesRepositorio meses) {
    this.meses = meses;
  }

  /** A ordenação é estável: no empate de intensidade, vale a ordem do pedido. */
  @Override
  public ResultadoDaAvaliacaoDeCompras avaliar(String mesId, List<CompraNomeada> compras) {
    Mes mes = meses.buscarPorId(mesId).orElseThrow(() -> new MesNaoEncontradoException(mesId));
    List<String> avisosDoMes =
        mes.nomesRepetidos().stream().map(MensagensDaAvaliacao::aviso).toList();
    List<CompraAvaliada> comprasAvaliadas =
        compras.stream()
            .map(compraNomeada -> avaliar(mes, compraNomeada))
            .sorted(MAIOR_INTENSIDADE_PRIMEIRO)
            .toList();
    return new ResultadoDaAvaliacaoDeCompras(avisosDoMes, comprasAvaliadas);
  }

  private static CompraAvaliada avaliar(Mes mes, CompraNomeada compraNomeada) {
    Compra compra = compraNomeada.compra();
    Avaliacao avaliacao = mes.avaliar(compra);
    return new CompraAvaliada(
        compraNomeada,
        MensagensDaAvaliacao.resultado(avaliacao, avisoDeIntensidade(avaliacao, compra)));
  }

  private static List<String> avisoDeIntensidade(Avaliacao avaliacao, Compra compra) {
    String aviso = MensagensDaAvaliacao.avisoDeIntensidade(avaliacao, compra);
    return aviso == null ? List.of() : List.of(aviso);
  }
}
