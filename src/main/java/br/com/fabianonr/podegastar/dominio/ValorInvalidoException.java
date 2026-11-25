package br.com.fabianonr.podegastar.dominio;

/** Entrada inválida na compra (R1, R12). Carrega o nome do campo que o 400 deve apontar. */
public class ValorInvalidoException extends RuntimeException {

  private final String campo;

  public ValorInvalidoException(String campo, String mensagem) {
    super(mensagem);
    this.campo = campo;
  }

  public String campo() {
    return campo;
  }
}
