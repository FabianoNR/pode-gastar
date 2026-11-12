package br.com.fabianonr.podegastar.dominio;

/** O grupo de itens candidatos a corte passou do limite que a busca de combinações suporta. */
public class ItensDemaisParaCombinarException extends RuntimeException {

  public ItensDemaisParaCombinarException(int maximoPorGrupo) {
    super(
        "itens demais para calcular sugestões de corte neste mês (máximo "
            + maximoPorGrupo
            + " por grupo)");
  }
}
