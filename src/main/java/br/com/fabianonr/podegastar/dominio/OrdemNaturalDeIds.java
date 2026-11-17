package br.com.fabianonr.podegastar.dominio;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ordem numérica natural de ids: trechos de dígitos comparam pelo valor, o resto pelo texto. Assim,
 * "item-2" vem antes de "item-10" (ADR 0011).
 */
final class OrdemNaturalDeIds {

  private static final Pattern TRECHO = Pattern.compile("[0-9]+|[^0-9]+");

  private OrdemNaturalDeIds() {}

  static int compararIds(String primeiro, String segundo) {
    List<String> trechosDoPrimeiro = trechos(primeiro);
    List<String> trechosDoSegundo = trechos(segundo);
    int quantidadeComum = Math.min(trechosDoPrimeiro.size(), trechosDoSegundo.size());
    for (int i = 0; i < quantidadeComum; i++) {
      int comparacao = compararTrechos(trechosDoPrimeiro.get(i), trechosDoSegundo.get(i));
      if (comparacao != 0) {
        return comparacao;
      }
    }
    int porQuantidadeDeTrechos = Integer.compare(trechosDoPrimeiro.size(), trechosDoSegundo.size());
    if (porQuantidadeDeTrechos != 0) {
      return porQuantidadeDeTrechos;
    }
    // Mesmo valor com zeros à esquerda diferentes ("item-02" e "item-2"): o texto decide.
    return primeiro.compareTo(segundo);
  }

  private static int compararTrechos(String primeiro, String segundo) {
    if (ehNumero(primeiro) && ehNumero(segundo)) {
      return new BigInteger(primeiro).compareTo(new BigInteger(segundo));
    }
    return primeiro.compareTo(segundo);
  }

  private static boolean ehNumero(String trecho) {
    char primeiroCaractere = trecho.charAt(0);
    return primeiroCaractere >= '0' && primeiroCaractere <= '9';
  }

  private static List<String> trechos(String id) {
    Matcher matcher = TRECHO.matcher(id);
    return matcher.results().map(resultado -> resultado.group()).toList();
  }
}
