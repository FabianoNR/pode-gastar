package br.com.fabianonr.podegastar.aplicacao;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.AvaliarCompra;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.ResultadoDaAvaliacao;
import br.com.fabianonr.podegastar.aplicacao.porta.saida.MesRepositorio;
import br.com.fabianonr.podegastar.dominio.Avaliacao;
import br.com.fabianonr.podegastar.dominio.Compra;
import br.com.fabianonr.podegastar.dominio.Item;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
    return new ResultadoDaAvaliacao(
        avaliacao,
        MensagensDaAvaliacao.motivo(avaliacao),
        avisos(avaliacao, compra),
        mensagensDosItensEmDiscussao(avaliacao));
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

  private static Map<String, String> mensagensDosItensEmDiscussao(Avaliacao avaliacao) {
    Stream<Item> itensSugeridos =
        avaliacao.sugestoes().stream().flatMap(sugestao -> sugestao.itens().stream());
    Map<String, String> mensagensPorIdDoItem = new HashMap<>();
    Stream.concat(itensSugeridos, avaliacao.listaCompleta().stream())
        .forEach(item -> guardarMensagemDoItem(mensagensPorIdDoItem, item));
    return mensagensPorIdDoItem;
  }

  /** Um item pode aparecer em mais de uma sugestão; a mensagem é a mesma, fica a primeira. */
  private static void guardarMensagemDoItem(Map<String, String> mensagensPorIdDoItem, Item item) {
    String mensagem = MensagensDaAvaliacao.mensagemDoItem(item);
    if (mensagem != null) {
      mensagensPorIdDoItem.putIfAbsent(item.id(), mensagem);
    }
  }
}
