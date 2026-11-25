package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.ItemDeTeste.umItem;
import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Intensidade da compra e itens mais desejados que ela, no mês de exemplo sem a bolsa. */
class IntensidadeDaCompraTest {

  private static Avaliacao avaliar(Mes mes, String valor, int intensidade) {
    return mes.avaliar(new Compra(valor(valor), Categoria.QUERO_FAZER, intensidade));
  }

  private static List<String> nomesMaisDesejados(Avaliacao avaliacao) {
    return avaliacao.itensMaisDesejadosQueACompra().stream().map(Item::nome).toList();
  }

  @Test
  @DisplayName("Compra sem intensidade vale 1 e não conta como informada")
  void compraSemIntensidadeValeUmENaoEhInformada() {
    Compra compra = new Compra(valor("300.00"), Categoria.QUERO_FAZER);

    assertThat(compra.intensidade()).isEqualTo(1);
    assertThat(compra.intensidadeInformada()).isFalse();
  }

  @Test
  @DisplayName("Compra com intensidade guarda o número e conta como informada")
  void compraComIntensidadeGuardaONumeroEEhInformada() {
    Compra compra = new Compra(valor("300.00"), Categoria.QUERO_FAZER, 4);

    assertThat(compra.intensidade()).isEqualTo(4);
    assertThat(compra.intensidadeInformada()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 6, -1})
  @DisplayName("Intensidade fora de 1 a 5 é recusada apontando o campo intensidade (CA70)")
  void intensidadeForaDeUmACincoEhRecusadaApontandoOCampo(int intensidade) {
    assertThatThrownBy(() -> new Compra(valor("300.00"), Categoria.QUERO_FAZER, intensidade))
        .isInstanceOf(ValorInvalidoException.class)
        .extracting(excecao -> ((ValorInvalidoException) excecao).campo())
        .isEqualTo("intensidade");
  }

  @Test
  @DisplayName("Valor inválido continua apontando o campo valor")
  void valorInvalidoApontaOCampoValor() {
    assertThatThrownBy(() -> new Compra(valor("0"), Categoria.QUERO_FAZER))
        .isInstanceOf(ValorInvalidoException.class)
        .extracting(excecao -> ((ValorInvalidoException) excecao).campo())
        .isEqualTo("valor");
  }

  @Test
  @DisplayName("Intensidade 5: nenhum item cortado é mais desejado, e a lista vem vazia (CA65)")
  void intensidadeCincoDeixaAListaVazia() {
    Avaliacao avaliacao = avaliar(base(), "300.00", 5);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("200");
    assertThat(avaliacao.itensMaisDesejadosQueACompra()).isEmpty();
  }

  @Test
  @DisplayName("Intensidade 1: jantar fora e bola de futebol (2) são mais desejados (CA66)")
  void intensidadeUmListaOsItensDeIntensidadeDois() {
    Avaliacao avaliacao = avaliar(base(), "300.00", 1);

    assertThat(nomesMaisDesejados(avaliacao)).containsExactly(JANTAR, BOLA);
    assertThat(avaliacao.itensMaisDesejadosQueACompra())
        .extracting(Item::intensidade)
        .containsOnly(2);
  }

  @Test
  @DisplayName("Sem intensidade informada vale 1 e a lista é a mesma da intensidade 1 (CA67)")
  void semIntensidadeAListaEhADaIntensidadeUm() {
    Avaliacao semIntensidade = base().avaliar(new Compra(valor("300.00"), Categoria.QUERO_FAZER));

    assertThat(semIntensidade).isEqualTo(avaliar(base(), "300.00", 1));
  }

  @Test
  @DisplayName("Intensidade 3: só a viagem (4) é mais desejada que a compra (CA68)")
  void intensidadeTresListaSoAViagem() {
    Avaliacao avaliacao = avaliar(base(), "600.00", 3);

    assertThat(nomesMaisDesejados(avaliacao)).containsExactly(VIAGEM);
    assertThat(avaliacao.itensMaisDesejadosQueACompra().get(0).intensidade()).isEqualTo(4);
  }

  @Test
  @DisplayName("Intensidade 4: a viagem tem intensidade igual e não entra (CA69)")
  void intensidadeIgualNaoEntra() {
    Avaliacao avaliacao = avaliar(base(), "600.00", 4);

    assertThat(avaliacao.itensMaisDesejadosQueACompra()).isEmpty();
  }

  @Test
  @DisplayName(
      "Lista completa também entra, sem o cartão protegido, e sugestoes segue vazia (CA72)")
  void listaCompletaTambemEntraSemOItemProtegido() {
    Avaliacao avaliacao = avaliar(base(), "1100.00", 3);

    assertThat(avaliacao.sugestoes()).isEmpty();
    assertThat(nomesMaisDesejados(avaliacao)).containsExactly(VIAGEM, MANUTENCAO);
  }

  @Test
  @DisplayName("Intensidade não muda as sugestões nem a ordem delas (R12)")
  void intensidadeNaoMudaAsSugestoes() {
    Avaliacao comIntensidadeUm = avaliar(base(), "600.00", 1);
    Avaliacao comIntensidadeCinco = avaliar(base(), "600.00", 5);

    assertThat(comIntensidadeUm.sugestoes()).isEqualTo(comIntensidadeCinco.sugestoes());
    assertThat(comIntensidadeUm.falta()).isEqualByComparingTo(comIntensidadeCinco.falta());
  }

  @Test
  @DisplayName("Item que aparece em mais de uma sugestão entra uma vez só, na ordem da primeira")
  void itemEmMaisDeUmaSugestaoEntraUmaVezSo() {
    Mes mes =
        new Mes(
            "mes-com-tres-opcoes",
            valor("1300"),
            valor("0"),
            valor("0"),
            List.of(
                umItem("item-1", "Jantar", Categoria.SERIA_LEGAL, 2, "100").criar(),
                umItem("item-2", "Passeio", Categoria.QUERO_FAZER, 1, "200").criar(),
                umItem("item-3", "Show", Categoria.QUERO_FAZER, 5, "100").criar(),
                umItem("item-4", "Conserto", Categoria.DEVO_FAZER, 2, "300").criar(),
                umItem("item-5", "Seguro", Categoria.DEVO_FAZER, 4, "500").criar()));

    Avaliacao avaliacao = mes.avaliar(new Compra(valor("700.00"), Categoria.DEVO_FAZER, 1));

    assertThat(avaliacao.sugestoes()).hasSize(3);
    assertThat(avaliacao.itensMaisDesejadosQueACompra())
        .extracting(Item::id)
        .containsExactly("item-1", "item-3", "item-4", "item-5");
  }

  @Test
  @DisplayName("APROVADO e NAO_CABE não trazem itens mais desejados, mesmo com intensidade (CA71)")
  void aprovadoENaoCabeNaoTrazemItensMaisDesejados() {
    assertThat(avaliar(base(), "80.00", 1).itensMaisDesejadosQueACompra()).isEmpty();
    assertThat(avaliar(base(), "1700.00", 1).itensMaisDesejadosQueACompra()).isEmpty();
  }

  @Test
  @DisplayName("Sem intensidade e sem item mais desejado que a compra, a lista vem vazia (CA73)")
  void semIntensidadeESemItemMaisDesejadoAListaVemVazia() {
    Mes mesComItensDeIntensidadeUm =
        new Mes(
            "mes-intensidade-um",
            valor("700"),
            valor("0"),
            valor("0"),
            List.of(
                umItem("item-1", "Jantar", Categoria.SERIA_LEGAL, 1, "200").criar(),
                umItem("item-2", "Bola", Categoria.QUERO_FAZER, 1, "300").criar()));

    Avaliacao avaliacao =
        mesComItensDeIntensidadeUm.avaliar(new Compra(valor("300.00"), Categoria.QUERO_FAZER));

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.itensMaisDesejadosQueACompra()).isEmpty();
  }
}
