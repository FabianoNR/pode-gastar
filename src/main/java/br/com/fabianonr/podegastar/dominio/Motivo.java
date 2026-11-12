package br.com.fabianonr.podegastar.dominio;

/** Por que a decisão saiu como saiu. O texto exibido ao usuário é montado pela aplicação. */
public enum Motivo {
  /** A compra é maior que o total que ainda sobra no mês (R4). */
  COMPRA_EXCEDE_TOTAL_RESTANTE,
  /** A compra não cabe no livre, mas cabe no total restante com cortes (R3). */
  CABE_COM_CORTES,
  /** Compra seria legal que os itens seria legal e em discussão não cobrem (R6 e R11). */
  FALTAM_ITENS_SERIA_LEGAL
}
