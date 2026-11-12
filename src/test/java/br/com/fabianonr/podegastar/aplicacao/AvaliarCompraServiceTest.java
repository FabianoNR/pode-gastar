package br.com.fabianonr.podegastar.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fabianonr.podegastar.adaptador.saida.memoria.MesEmMemoriaRepositorio;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.ResultadoDaAvaliacao;
import br.com.fabianonr.podegastar.dominio.Categoria;
import br.com.fabianonr.podegastar.dominio.Compra;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Os textos de negócio são montados pelo serviço e vêm prontos no resultado. */
class AvaliarCompraServiceTest {

  private final AvaliarCompraService servico =
      new AvaliarCompraService(new MesEmMemoriaRepositorio());

  private ResultadoDaAvaliacao avaliar(String valor, Categoria categoria) {
    return servico.avaliar(
        MesEmMemoriaRepositorio.MES_EXEMPLO, new Compra(new BigDecimal(valor), categoria));
  }

  @Test
  @DisplayName("Compra aprovada não tem motivo, e os avisos vêm como lista vazia")
  void aprovadoSemMotivoEComAvisosVazios() {
    ResultadoDaAvaliacao resultado = avaliar("80.00", Categoria.QUERO_FAZER);

    assertThat(resultado.motivo()).isNull();
    assertThat(resultado.avisos()).isEmpty();
  }

  @Test
  @DisplayName("Compra maior que o total restante traz o motivo pronto (CA6)")
  void motivoQuandoExcedeOTotalRestante() {
    ResultadoDaAvaliacao resultado = avaliar("1700.00", Categoria.QUERO_FAZER);

    assertThat(resultado.motivo()).isEqualTo("compra excede o total que ainda sobra no mês");
  }

  @Test
  @DisplayName("Compra seria legal sem itens seria legal suficientes traz o motivo pronto (R11)")
  void motivoQuandoFaltamItensSeriaLegal() {
    ResultadoDaAvaliacao resultado = avaliar("1000.00", Categoria.SERIA_LEGAL);

    assertThat(resultado.motivo())
        .isEqualTo("não há itens seria legal suficientes para cobrir a compra");
  }

  @Test
  @DisplayName("Sem combinação que cubra, o motivo diz quanto ainda faltaria (R7)")
  void motivoComValorQueAindaFaltaria() {
    ResultadoDaAvaliacao resultado = avaliar("1600.00", Categoria.DEVO_FAZER);

    assertThat(resultado.motivo())
        .isEqualTo(
            "compra não cabe no livre; cabe no total restante com cortes;"
                + " mesmo cortando todos os itens elegíveis ainda faltariam 600.00");
  }
}
