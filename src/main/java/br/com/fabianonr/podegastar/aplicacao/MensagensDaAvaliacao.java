package br.com.fabianonr.podegastar.aplicacao;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.ResultadoDaAvaliacao;
import br.com.fabianonr.podegastar.dominio.Avaliacao;
import br.com.fabianonr.podegastar.dominio.Compra;
import br.com.fabianonr.podegastar.dominio.Dinheiro;
import br.com.fabianonr.podegastar.dominio.Item;
import br.com.fabianonr.podegastar.dominio.NomeRepetido;
import br.com.fabianonr.podegastar.dominio.StatusItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Único lugar que monta os textos de negócio exibidos ao usuário (spec v7, Mensagens ao usuário).
 */
final class MensagensDaAvaliacao {

  private MensagensDaAvaliacao() {}

  /** A avaliação com o motivo, os avisos dados e a mensagem de cada item em discussão. */
  static ResultadoDaAvaliacao resultado(Avaliacao avaliacao, List<String> avisos) {
    return new ResultadoDaAvaliacao(
        avaliacao, motivo(avaliacao), avisos, mensagensDosItensEmDiscussao(avaliacao));
  }

  static String motivo(Avaliacao avaliacao) {
    if (avaliacao.motivo() == null) {
      return null;
    }
    return switch (avaliacao.motivo()) {
      case COMPRA_EXCEDE_TOTAL_RESTANTE -> "compra excede o total que ainda sobra no mês";
      case FALTAM_ITENS_SERIA_LEGAL -> "não há itens seria legal suficientes para cobrir a compra";
      case CABE_COM_CORTES -> motivoDeCabeComCortes(avaliacao);
    };
  }

  static String aviso(NomeRepetido nomeRepetido) {
    String nomesEntreAspas =
        nomeRepetido.nomes().stream()
            .map(nome -> "\"" + nome + "\"")
            .collect(Collectors.joining(", "));
    return "Nome repetido neste mês: "
        + nomesEntreAspas
        + " (ids: "
        + String.join(", ", nomeRepetido.ids())
        + "). São itens diferentes.";
  }

  /**
   * Aviso de que a compra é menos desejada que os itens a cortar (R12); nada quando nenhum item é
   * mais desejado que ela.
   */
  static String avisoDeIntensidade(Avaliacao avaliacao, Compra compra) {
    if (avaliacao.itensMaisDesejadosQueACompra().isEmpty()) {
      return null;
    }
    String nomesEntreAspas =
        avaliacao.itensMaisDesejadosQueACompra().stream()
            .map(item -> "\"" + item.nome() + "\"")
            .collect(Collectors.joining(", "));
    if (compra.intensidadeInformada()) {
      return "A compra (intensidade "
          + compra.intensidade()
          + ") é menos desejada que os itens a cortar: "
          + nomesEntreAspas
          + ".";
    }
    return "A intensidade da compra não foi informada e foi considerada "
        + compra.intensidade()
        + ". A compra é menos desejada que os itens a cortar: "
        + nomesEntreAspas
        + ".";
  }

  /** Texto do item em discussão (R8); nada para os demais itens. */
  static String mensagemDoItem(Item item) {
    if (item.status() != StatusItem.EM_DISCUSSAO || item.decisorResponsavel() == null) {
      return null;
    }
    return "aguardando decisão de " + item.decisorResponsavel();
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
    String mensagem = mensagemDoItem(item);
    if (mensagem != null) {
      mensagensPorIdDoItem.putIfAbsent(item.id(), mensagem);
    }
  }

  private static String motivoDeCabeComCortes(Avaliacao avaliacao) {
    String motivo = "compra não cabe no livre; cabe no total restante com cortes";
    if (avaliacao.aindaFaltaria() == null) {
      return motivo;
    }
    return motivo
        + "; mesmo cortando todos os itens elegíveis ainda faltariam "
        + Dinheiro.formatar(avaliacao.aindaFaltaria());
  }
}
