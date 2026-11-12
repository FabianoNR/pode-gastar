package br.com.fabianonr.podegastar.aplicacao.porta.entrada;

import br.com.fabianonr.podegastar.dominio.Avaliacao;
import java.util.List;
import java.util.Map;

/**
 * A avaliação do domínio com os textos de negócio já montados pela aplicação. A tela só exibe.
 *
 * @param motivo texto do motivo da decisão; ausente quando a compra é aprovada
 * @param avisos um texto por nome repetido no mês; lista vazia quando não há nada a avisar
 * @param mensagensPorIdDoItem texto de cada item em discussão, pelo id do item
 */
public record ResultadoDaAvaliacao(
    Avaliacao avaliacao,
    String motivo,
    List<String> avisos,
    Map<String, String> mensagensPorIdDoItem) {

  public ResultadoDaAvaliacao {
    avisos = List.copyOf(avisos);
    mensagensPorIdDoItem = Map.copyOf(mensagensPorIdDoItem);
  }

  /**
   * Mensagem do item, ou nada quando o item não tem mensagem (por exemplo, não está em discussão).
   */
  public String mensagemDoItem(String idDoItem) {
    return mensagensPorIdDoItem.get(idDoItem);
  }
}
