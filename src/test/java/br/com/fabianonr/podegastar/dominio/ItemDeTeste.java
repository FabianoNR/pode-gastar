package br.com.fabianonr.podegastar.dominio;

import java.math.BigDecimal;

/**
 * Construtor único de itens para os testes. Parte de um item planejado e {@code PENDENTE}, sem
 * valor realizado e sem decisor responsável; cada teste muda só o que importa para ele.
 */
public final class ItemDeTeste {

  private final String id;
  private final String nome;
  private final Categoria categoria;
  private final int intensidade;
  private final BigDecimal valorEstimado;
  private boolean planejado = true;
  private StatusItem status = StatusItem.PENDENTE;
  private BigDecimal valorRealizado;
  private String decisorResponsavel;

  private ItemDeTeste(
      String id, String nome, Categoria categoria, int intensidade, BigDecimal valorEstimado) {
    this.id = id;
    this.nome = nome;
    this.categoria = categoria;
    this.intensidade = intensidade;
    this.valorEstimado = valorEstimado;
  }

  public static ItemDeTeste umItem(
      String id, String nome, Categoria categoria, int intensidade, String valorEstimado) {
    return new ItemDeTeste(id, nome, categoria, intensidade, new BigDecimal(valorEstimado));
  }

  public ItemDeTeste comStatus(StatusItem novoStatus) {
    this.status = novoStatus;
    return this;
  }

  public ItemDeTeste comValorRealizado(String valor) {
    this.valorRealizado = new BigDecimal(valor);
    return this;
  }

  /** Item não planejado, ainda não decidido, à espera do decisor informado. */
  public ItemDeTeste emDiscussaoAguardando(String decisor) {
    this.planejado = false;
    this.status = StatusItem.EM_DISCUSSAO;
    this.decisorResponsavel = decisor;
    return this;
  }

  public Item criar() {
    return new Item(
        id,
        nome,
        categoria,
        intensidade,
        planejado,
        status,
        valorEstimado,
        valorRealizado,
        decisorResponsavel);
  }
}
