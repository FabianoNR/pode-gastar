package br.com.fabianonr.podegastar.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fabianonr.podegastar.adaptador.saida.memoria.MesEmMemoriaRepositorio;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.ResultadoDaAvaliacao;
import br.com.fabianonr.podegastar.dominio.Categoria;
import br.com.fabianonr.podegastar.dominio.Compra;
import br.com.fabianonr.podegastar.dominio.ItemDeTeste;
import br.com.fabianonr.podegastar.dominio.Mes;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O serviço monta o aviso de intensidade e o põe em avisos (R12, Mensagens ao usuário). */
class MensagemDeIntensidadeTest {

  private static final String AVISO_COM_INTENSIDADE_UM =
      "A compra (intensidade 1) é menos desejada que os itens a cortar:"
          + " \"Jantar fora no mês\", \"Bola de futebol\".";

  private final AvaliarCompraService servico =
      new AvaliarCompraService(new MesEmMemoriaRepositorio());

  private ResultadoDaAvaliacao avaliar(Compra compra) {
    return servico.avaliar(MesEmMemoriaRepositorio.MES_EXEMPLO, compra);
  }

  private static Compra compra(String valor, Categoria categoria) {
    return new Compra(new BigDecimal(valor), categoria);
  }

  private static Compra compra(String valor, Categoria categoria, int intensidade) {
    return new Compra(new BigDecimal(valor), categoria, intensidade);
  }

  @Test
  @DisplayName("Com intensidade 1 informada, avisos diz que a compra é menos desejada (CA66)")
  void intensidadeInformadaTrazOAvisoDeMenosDesejada() {
    ResultadoDaAvaliacao resultado = avaliar(compra("300.00", Categoria.QUERO_FAZER, 1));

    assertThat(resultado.avisos()).containsExactly(AVISO_COM_INTENSIDADE_UM);
  }

  @Test
  @DisplayName("Sem intensidade, avisos diz que ela foi considerada 1 (CA67)")
  void semIntensidadeOAvisoDizQueFoiConsideradaUm() {
    ResultadoDaAvaliacao resultado = avaliar(compra("300.00", Categoria.QUERO_FAZER));

    assertThat(resultado.avisos())
        .containsExactly(
            "A intensidade da compra não foi informada e foi considerada 1."
                + " A compra é menos desejada que os itens a cortar:"
                + " \"Jantar fora no mês\", \"Bola de futebol\".");
  }

  @Test
  @DisplayName("Intensidade 5: nenhum item é mais desejado, e avisos não traz mensagem (CA65)")
  void semItemMaisDesejadoNaoHaAviso() {
    ResultadoDaAvaliacao resultado = avaliar(compra("300.00", Categoria.QUERO_FAZER, 5));

    assertThat(resultado.avisos()).isEmpty();
  }

  @Test
  @DisplayName("O aviso cita os nomes dos itens mais desejados, na ordem da lista (CA68)")
  void oAvisoCitaSoOsItensMaisDesejados() {
    ResultadoDaAvaliacao resultado = avaliar(compra("600.00", Categoria.QUERO_FAZER, 3));

    assertThat(resultado.avisos())
        .containsExactly(
            "A compra (intensidade 3) é menos desejada que os itens a cortar:"
                + " \"Viagem de fim de semana\".");
  }

  @Test
  @DisplayName("APROVADO e NAO_CABE não trazem o aviso de intensidade (CA71)")
  void aprovadoENaoCabeNaoTrazemAviso() {
    assertThat(avaliar(compra("80.00", Categoria.QUERO_FAZER, 1)).avisos()).isEmpty();
    assertThat(avaliar(compra("1700.00", Categoria.QUERO_FAZER, 1)).avisos()).isEmpty();
  }

  @Test
  @DisplayName(
      "Sem intensidade e sem item mais desejado que a compra, avisos fica sem mensagem (CA73)")
  void semIntensidadeESemItemMaisDesejadoNaoHaAviso() {
    Mes mes =
        new Mes(
            "mes-intensidade-um",
            new BigDecimal("700"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(
                ItemDeTeste.umItem("item-1", "Jantar", Categoria.SERIA_LEGAL, 1, "200").criar(),
                ItemDeTeste.umItem("item-2", "Bola", Categoria.QUERO_FAZER, 1, "300").criar()));
    AvaliarCompraService servicoDoMes = new AvaliarCompraService(mesId -> Optional.of(mes));

    ResultadoDaAvaliacao resultado =
        servicoDoMes.avaliar("mes-intensidade-um", compra("300.00", Categoria.QUERO_FAZER));

    assertThat(resultado.avisos()).isEmpty();
  }
}
