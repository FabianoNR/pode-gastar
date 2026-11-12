package br.com.fabianonr.podegastar.adaptador.saida.memoria;

import static br.com.fabianonr.podegastar.dominio.Categoria.DEVO_FAZER;
import static br.com.fabianonr.podegastar.dominio.Categoria.QUERO_FAZER;
import static br.com.fabianonr.podegastar.dominio.Categoria.SERIA_LEGAL;

import br.com.fabianonr.podegastar.aplicacao.porta.saida.MesRepositorio;
import br.com.fabianonr.podegastar.dominio.Categoria;
import br.com.fabianonr.podegastar.dominio.Item;
import br.com.fabianonr.podegastar.dominio.Mes;
import br.com.fabianonr.podegastar.dominio.StatusItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Mock em memória (ADR 0003): guarda só o mês de exemplo da spec, sem a bolsa. */
public class MesEmMemoriaRepositorio implements MesRepositorio {

  public static final String MES_EXEMPLO = "mes-exemplo";

  private final Map<String, Mes> meses = Map.of(MES_EXEMPLO, mesExemplo());

  @Override
  public Optional<Mes> buscarPorId(String mesId) {
    return Optional.ofNullable(meses.get(mesId));
  }

  private static Mes mesExemplo() {
    return new Mes(
        MES_EXEMPLO,
        new BigDecimal("6000"),
        new BigDecimal("3400"),
        new BigDecimal("1000"),
        List.of(
            pendente("item-1", Item.NOME_CARTAO, DEVO_FAZER, 5, "600"),
            pendente("item-2", "Manutenção do carro", DEVO_FAZER, 5, "200"),
            pendente("item-3", "Viagem de fim de semana", QUERO_FAZER, 4, "300"),
            pendente("item-4", "Item para casa", QUERO_FAZER, 3, "200"),
            pendente("item-5", "Jantar fora no mês", SERIA_LEGAL, 2, "120"),
            pendente("item-6", "Bola de futebol", SERIA_LEGAL, 2, "80")));
  }

  private static Item pendente(
      String id, String nome, Categoria categoria, int intensidade, String valor) {
    return new Item(
        id,
        nome,
        categoria,
        intensidade,
        true,
        StatusItem.PENDENTE,
        new BigDecimal(valor),
        null,
        null);
  }
}
