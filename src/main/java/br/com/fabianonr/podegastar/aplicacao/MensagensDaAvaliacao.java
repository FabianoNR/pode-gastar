package br.com.fabianonr.podegastar.aplicacao;

import br.com.fabianonr.podegastar.dominio.Avaliacao;
import br.com.fabianonr.podegastar.dominio.Dinheiro;
import br.com.fabianonr.podegastar.dominio.Item;
import br.com.fabianonr.podegastar.dominio.NomeRepetido;
import br.com.fabianonr.podegastar.dominio.StatusItem;
import java.util.stream.Collectors;

/**
 * Único lugar que monta os textos de negócio exibidos ao usuário (spec v7, Mensagens ao usuário).
 */
final class MensagensDaAvaliacao {

  private MensagensDaAvaliacao() {}

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

  /** Texto do item em discussão (R8); nada para os demais itens. */
  static String mensagemDoItem(Item item) {
    if (item.status() != StatusItem.EM_DISCUSSAO || item.decisorResponsavel() == null) {
      return null;
    }
    return "aguardando decisão de " + item.decisorResponsavel();
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
