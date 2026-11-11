package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Itens que seriam cortados e o valor que eles cobrem. Só mostra; não executa (R10). */
public record Sugestao(Opcao opcao, List<Item> itens, BigDecimal cobre) {

  public Sugestao {
    itens = List.copyOf(itens);
  }

  static Sugestao de(Opcao opcao, List<Item> itens) {
    return new Sugestao(opcao, itens, PlanoDeCortes.somaDeCortes(itens));
  }
}
