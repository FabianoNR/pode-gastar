package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.dominio.ValorInvalidoException;
import java.math.BigDecimal;

/** Formato do valor na requisição: no máximo duas casas decimais (contrato da API). */
final class CasasDecimaisDoValor {

  private static final int MAXIMO_DE_CASAS = 2;

  private CasasDecimaisDoValor() {}

  static void exigirNoMaximoDuas(BigDecimal valor, String campo) {
    if (valor.stripTrailingZeros().scale() > MAXIMO_DE_CASAS) {
      throw new ValorInvalidoException(campo, "valor deve ter no máximo duas casas decimais");
    }
  }
}
