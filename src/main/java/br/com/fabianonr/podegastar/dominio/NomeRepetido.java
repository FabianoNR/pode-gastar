package br.com.fabianonr.podegastar.dominio;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Itens diferentes (ids diferentes) que têm o mesmo nome no mês. Só informa: nenhum item é removido
 * nem escondido por causa disso.
 *
 * @param nomes os nomes como foram escritos, sem repetição, na ordem dos ids
 * @param ids os ids dos itens envolvidos, em ordem alfabética
 */
public record NomeRepetido(List<String> nomes, List<String> ids) {

  public NomeRepetido {
    nomes = List.copyOf(nomes);
    ids = List.copyOf(ids);
  }

  /** Agrupa os itens pelo nome, ignorando maiúsculas e espaços no início e no fim. */
  static List<NomeRepetido> encontrarEm(List<Item> itens) {
    Map<String, List<Item>> itensPorNome =
        itens.stream()
            .collect(
                Collectors.groupingBy(
                    item -> nomeParaComparar(item.nome()), TreeMap::new, Collectors.toList()));
    return itensPorNome.values().stream()
        .filter(itensComOMesmoNome -> itensComOMesmoNome.size() > 1)
        .map(NomeRepetido::de)
        .toList();
  }

  public String mensagem() {
    String nomesEntreAspas =
        nomes.stream().map(nome -> "\"" + nome + "\"").collect(Collectors.joining(", "));
    return "Nome repetido neste mês: "
        + nomesEntreAspas
        + " (ids: "
        + String.join(", ", ids)
        + "). São itens diferentes.";
  }

  private static NomeRepetido de(List<Item> itensComOMesmoNome) {
    List<Item> emOrdemDeId =
        itensComOMesmoNome.stream().sorted(Comparator.comparing(Item::id)).toList();
    return new NomeRepetido(
        emOrdemDeId.stream().map(Item::nome).distinct().toList(),
        emOrdemDeId.stream().map(Item::id).toList());
  }

  private static String nomeParaComparar(String nome) {
    return nome.strip().toLowerCase(Locale.ROOT);
  }
}
