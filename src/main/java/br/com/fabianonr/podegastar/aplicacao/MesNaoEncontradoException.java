package br.com.fabianonr.podegastar.aplicacao;

public class MesNaoEncontradoException extends RuntimeException {

  public MesNaoEncontradoException(String mesId) {
    super("mês não encontrado: " + mesId);
  }
}
