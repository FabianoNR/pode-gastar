package br.com.fabianonr.podegastar.aplicacao;

import static br.com.fabianonr.podegastar.dominio.ItemDeTeste.umItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fabianonr.podegastar.adaptador.saida.memoria.MesEmMemoriaRepositorio;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.CompraAvaliada;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.CompraNomeada;
import br.com.fabianonr.podegastar.aplicacao.porta.entrada.ResultadoDaAvaliacaoDeCompras;
import br.com.fabianonr.podegastar.aplicacao.porta.saida.MesRepositorio;
import br.com.fabianonr.podegastar.dominio.Categoria;
import br.com.fabianonr.podegastar.dominio.Compra;
import br.com.fabianonr.podegastar.dominio.Decisao;
import br.com.fabianonr.podegastar.dominio.Item;
import br.com.fabianonr.podegastar.dominio.ItensDemaisParaCombinarException;
import br.com.fabianonr.podegastar.dominio.Mes;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Consulta de vários itens na camada de aplicação: ordem, avisos e erros da lista. */
class AvaliarComprasServiceTest {

  private static final String MES_EXEMPLO = MesEmMemoriaRepositorio.MES_EXEMPLO;

  private final AvaliarComprasService servicoDoMesExemplo =
      new AvaliarComprasService(new MesEmMemoriaRepositorio());

  private static CompraNomeada semIntensidade(String nome, String valor, Categoria categoria) {
    return new CompraNomeada(nome, new Compra(new BigDecimal(valor), categoria));
  }

  private static CompraNomeada comIntensidade(
      String nome, String valor, Categoria categoria, int intensidade) {
    return new CompraNomeada(nome, new Compra(new BigDecimal(valor), categoria, intensidade));
  }

  private static List<String> nomesNaOrdem(ResultadoDaAvaliacaoDeCompras resultado) {
    return resultado.comprasAvaliadas().stream()
        .map(compraAvaliada -> compraAvaliada.compraNomeada().nome())
        .toList();
  }

  private static CompraAvaliada avaliadaComNome(
      ResultadoDaAvaliacaoDeCompras resultado, String nome) {
    return resultado.comprasAvaliadas().stream()
        .filter(compraAvaliada -> compraAvaliada.compraNomeada().nome().equals(nome))
        .findFirst()
        .orElseThrow();
  }

  @Test
  @DisplayName(
      "Cada compra é avaliada sozinha e a lista vem da maior intensidade para a menor (CA74)")
  void avaliaCadaCompraSozinhaEOrdenaPelaIntensidade() {
    ResultadoDaAvaliacaoDeCompras resultado =
        servicoDoMesExemplo.avaliar(
            MES_EXEMPLO,
            List.of(
                semIntensidade("Livro", "80.00", Categoria.QUERO_FAZER),
                comIntensidade("Tênis", "300.00", Categoria.QUERO_FAZER, 5),
                comIntensidade("Viagem extra", "1700.00", Categoria.QUERO_FAZER, 3)));

    assertThat(nomesNaOrdem(resultado)).containsExactly("Tênis", "Viagem extra", "Livro");
    CompraAvaliada tenis = avaliadaComNome(resultado, "Tênis");
    assertThat(tenis.resultado().avaliacao().decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(tenis.resultado().avaliacao().falta()).isEqualByComparingTo("200");
    CompraAvaliada viagemExtra = avaliadaComNome(resultado, "Viagem extra");
    assertThat(viagemExtra.resultado().avaliacao().decisao()).isEqualTo(Decisao.NAO_CABE);
    assertThat(viagemExtra.resultado().avaliacao().falta()).isEqualByComparingTo("100");
    assertThat(viagemExtra.resultado().motivo())
        .isEqualTo("compra excede o total que ainda sobra no mês");
    CompraAvaliada livro = avaliadaComNome(resultado, "Livro");
    assertThat(livro.resultado().avaliacao().decisao()).isEqualTo(Decisao.APROVADO);
    assertThat(livro.resultado().avaliacao().saldoRestante()).isEqualByComparingTo("20");
    assertThat(resultado.avisosDoMes()).isEmpty();
  }

  @Test
  @DisplayName("No empate de intensidade, vale a ordem do pedido (CA75)")
  void empateDeIntensidadeMantemAOrdemDoPedido() {
    ResultadoDaAvaliacaoDeCompras resultado =
        servicoDoMesExemplo.avaliar(
            MES_EXEMPLO,
            List.of(
                semIntensidade("C", "50.00", Categoria.QUERO_FAZER),
                comIntensidade("A", "80.00", Categoria.QUERO_FAZER, 2),
                comIntensidade("B", "150.00", Categoria.SERIA_LEGAL, 2)));

    assertThat(nomesNaOrdem(resultado)).containsExactly("A", "B", "C");
  }

  @Test
  @DisplayName("O aviso de intensidade fica só na avaliação da compra que o gerou (CA76)")
  void avisoDeIntensidadeFicaNaPropriaAvaliacao() {
    ResultadoDaAvaliacaoDeCompras resultado =
        servicoDoMesExemplo.avaliar(
            MES_EXEMPLO,
            List.of(
                semIntensidade("X", "300.00", Categoria.QUERO_FAZER),
                comIntensidade("Y", "300.00", Categoria.QUERO_FAZER, 5)));

    assertThat(avaliadaComNome(resultado, "X").resultado().avisos())
        .containsExactly(
            "A intensidade da compra não foi informada e foi considerada 1."
                + " A compra é menos desejada que os itens a cortar:"
                + " \"Jantar fora no mês\", \"Bola de futebol\".");
    assertThat(avaliadaComNome(resultado, "Y").resultado().avisos()).isEmpty();
    assertThat(resultado.avisosDoMes()).isEmpty();
  }

  @Test
  @DisplayName("O aviso de nome repetido do mês sai uma vez na lista e em nenhuma avaliação (CA77)")
  void avisoDeNomeRepetidoSaiUmaVezSoNaLista() {
    MesRepositorio mesComNomeRepetido =
        mesId ->
            Optional.of(
                mes(
                    mesId,
                    "1000",
                    seriaLegal("item-5", "Jantar fora"),
                    seriaLegal("item-9", "Jantar fora")));

    ResultadoDaAvaliacaoDeCompras resultado =
        new AvaliarComprasService(mesComNomeRepetido)
            .avaliar(
                "mes-com-nome-repetido",
                List.of(
                    semIntensidade("Livro", "80.00", Categoria.QUERO_FAZER),
                    semIntensidade("Caderno", "30.00", Categoria.QUERO_FAZER)));

    assertThat(resultado.avisosDoMes())
        .containsExactly(
            "Nome repetido neste mês: \"Jantar fora\" (ids: item-5, item-9). São itens diferentes.");
    assertThat(resultado.comprasAvaliadas())
        .allSatisfy(compraAvaliada -> assertThat(compraAvaliada.resultado().avisos()).isEmpty());
  }

  @Test
  @DisplayName("O mês é lido uma vez só para a lista toda")
  void leOMesUmaVezSo() {
    AtomicInteger leiturasDoMes = new AtomicInteger();
    MesEmMemoriaRepositorio repositorioDoExemplo = new MesEmMemoriaRepositorio();
    MesRepositorio repositorioQueConta =
        mesId -> {
          leiturasDoMes.incrementAndGet();
          return repositorioDoExemplo.buscarPorId(mesId);
        };

    new AvaliarComprasService(repositorioQueConta)
        .avaliar(
            MES_EXEMPLO,
            List.of(
                semIntensidade("Livro", "80.00", Categoria.QUERO_FAZER),
                semIntensidade("Tênis", "300.00", Categoria.QUERO_FAZER),
                semIntensidade("Viagem extra", "1700.00", Categoria.QUERO_FAZER)));

    assertThat(leiturasDoMes).hasValue(1);
  }

  @Test
  @DisplayName("Mês inexistente derruba a lista toda (CA81)")
  void mesInexistenteDerrubaALista() {
    List<CompraNomeada> compras = List.of(semIntensidade("Livro", "80.00", Categoria.QUERO_FAZER));

    assertThatThrownBy(() -> servicoDoMesExemplo.avaliar("nao-existe", compras))
        .isInstanceOf(MesNaoEncontradoException.class)
        .hasMessage("mês não encontrado: nao-existe");
  }

  @Test
  @DisplayName("Qualquer compra no limite de 20 itens por grupo derruba a lista toda (CA82)")
  void compraNoLimiteDeItensDerrubaALista() {
    List<Item> vinteEUmEmDiscussao =
        IntStream.rangeClosed(1, 21)
            .mapToObj(numero ->
                umItem("item-" + numero, "Pedido " + numero, Categoria.QUERO_FAZER, 1, "10")
                    .emDiscussaoAguardando("marido")
                    .criar())
            .toList();
    MesRepositorio mesComItensDemais =
        mesId ->
            Optional.of(
                new Mes(
                    mesId,
                    new BigDecimal("1000"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    vinteEUmEmDiscussao));
    List<CompraNomeada> compras =
        List.of(
            semIntensidade("A", "100.00", Categoria.QUERO_FAZER),
            semIntensidade("B", "900.00", Categoria.QUERO_FAZER));

    assertThatThrownBy(
            () -> new AvaliarComprasService(mesComItensDemais).avaliar("mes-cheio", compras))
        .isInstanceOf(ItensDemaisParaCombinarException.class);
  }

  private static Item seriaLegal(String id, String nome) {
    return umItem(id, nome, Categoria.SERIA_LEGAL, 2, "100").criar();
  }

  private static Mes mes(String mesId, String orcamentoVariavel, Item... itens) {
    return new Mes(
        mesId, new BigDecimal(orcamentoVariavel), BigDecimal.ZERO, BigDecimal.ZERO, List.of(itens));
  }
}
