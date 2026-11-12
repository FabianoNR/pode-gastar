package br.com.fabianonr.podegastar.dominio;

/** Entrada inválida na compra (R1: valor menor ou igual a zero). */
public class ValorInvalidoException extends RuntimeException {

  public ValorInvalidoException(String mensagem) {
    super(mensagem);
  }
}
