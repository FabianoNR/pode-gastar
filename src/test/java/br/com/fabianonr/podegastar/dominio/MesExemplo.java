package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.Categoria.DEVO_FAZER;
import static br.com.fabianonr.podegastar.dominio.Categoria.QUERO_FAZER;
import static br.com.fabianonr.podegastar.dominio.Categoria.SERIA_LEGAL;
import static br.com.fabianonr.podegastar.dominio.StatusItem.EM_DISCUSSAO;
import static br.com.fabianonr.podegastar.dominio.StatusItem.PENDENTE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Fixture com os valores da tabela "Mês de exemplo" da spec (mesId = mes-exemplo). */
final class MesExemplo {

  static final String MES_ID = "mes-exemplo";

  static final String CARTAO = "Cartão de crédito";
  static final String MANUTENCAO = "Manutenção do carro";
  static final String VIAGEM = "Viagem de fim de semana";
  static final String ITEM_CASA = "Item para casa";
  static final String JANTAR = "Jantar fora no mês";
  static final String BOLA = "Bola de futebol";
  static final String BOLSA = "Bolsa";

  private MesExemplo() {}

  static BigDecimal valor(String texto) {
    return new BigDecimal(texto);
  }

  static Item pendente(
      String id, String nome, Categoria categoria, int intensidade, String estimado) {
    return new Item(id, nome, categoria, intensidade, true, PENDENTE, valor(estimado), null, null);
  }

  /** Item em discussão cujo id é o próprio nome, para testes que não precisam de id. */
  static Item emDiscussao(
      String nome, Categoria categoria, int intensidade, String estimado, String decisor) {
    return emDiscussao(nome, nome, categoria, intensidade, estimado, decisor);
  }

  static Item emDiscussao(
      String id,
      String nome,
      Categoria categoria,
      int intensidade,
      String estimado,
      String decisor) {
    return new Item(
        id, nome, categoria, intensidade, false, EM_DISCUSSAO, valor(estimado), null, decisor);
  }

  static Item viagem(StatusItem status, String realizado) {
    return new Item(
        "item-3", VIAGEM, QUERO_FAZER, 4, true, status, valor("300"), valor(realizado), null);
  }

  static Item viagemPendente() {
    return pendente("item-3", VIAGEM, QUERO_FAZER, 4, "300");
  }

  static Item bolsa() {
    return emDiscussao("item-7", BOLSA, QUERO_FAZER, 3, "90", "marido");
  }

  /** Mês base, sem a bolsa: total restante 1600, livre 100. */
  static Mes base() {
    return com(viagemPendente());
  }

  /** Mês base com a viagem substituída pela informada (PARCIAL ou REALIZADO). */
  static Mes comViagem(Item viagem) {
    return com(viagem);
  }

  /** Mês base mais itens adicionais (viagem padrão pendente). */
  static Mes comExtras(Item... extras) {
    List<Item> itens = new ArrayList<>(itensBase(viagemPendente()));
    itens.addAll(List.of(extras));
    return mes(itens);
  }

  private static Mes com(Item viagem) {
    return mes(itensBase(viagem));
  }

  private static List<Item> itensBase(Item viagem) {
    return Stream.of(
            pendente("item-1", CARTAO, DEVO_FAZER, 5, "600"),
            pendente("item-2", MANUTENCAO, DEVO_FAZER, 5, "200"),
            viagem,
            pendente("item-4", ITEM_CASA, QUERO_FAZER, 3, "200"),
            pendente("item-5", JANTAR, SERIA_LEGAL, 2, "120"),
            pendente("item-6", BOLA, SERIA_LEGAL, 2, "80"))
        .toList();
  }

  private static Mes mes(List<Item> itens) {
    return new Mes(MES_ID, valor("6000"), valor("3400"), valor("1000"), itens);
  }
}
