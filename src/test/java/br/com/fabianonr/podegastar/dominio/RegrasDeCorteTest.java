package br.com.fabianonr.podegastar.dominio;

import static br.com.fabianonr.podegastar.dominio.MesExemplo.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regras de R6 e R7 que nenhum critério de aceite exercita. Os cenários são derivados do texto da
 * spec (v8).
 */
class RegrasDeCorteTest {

  private static Avaliacao avaliar(String valor, Categoria categoria) {
    return base().avaliar(new Compra(valor(valor), categoria));
  }

  private static List<String> nomes(Sugestao sugestao) {
    return sugestao.itens().stream().map(Item::nome).toList();
  }

  private static List<String> ids(Sugestao sugestao) {
    return sugestao.itens().stream().map(Item::id).toList();
  }

  private static Mes mesCom(String orcamentoVariavel, Item... itens) {
    return new Mes(MES_ID, valor(orcamentoVariavel), valor("0"), valor("0"), List.of(itens));
  }

  @Test
  @DisplayName(
      "Compra SERIA_LEGAL que os SERIA_LEGAL não cobrem é NAO_CABE, sem trocar por QUERO (R6)")
  void compraSeriaLegalSemCoberturaDeSeriaLegalNaoCabe() {
    // livre 100, falta 900; os SERIA_LEGAL cobrem só 200, então ainda faltariam 700.
    Avaliacao avaliacao = avaliar("1000.00", Categoria.SERIA_LEGAL);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.NAO_CABE);
    assertThat(avaliacao.motivo()).isEqualTo(Motivo.FALTAM_ITENS_SERIA_LEGAL);
    assertThat(avaliacao.falta()).isEqualByComparingTo("700");
    assertThat(avaliacao.sugestoes()).isEmpty();
  }

  @Test
  @DisplayName("Opção 1: todos SERIA_LEGAL e QUERO_FAZER mais um DEVO_FAZER que fecha o valor (R7)")
  void compraDevoFazerSemCoberturaUsaOpcaoComUmDevoFazerQueFechaOValor() {
    // falta 900; SERIA + QUERO cobrem 700; a manutenção (200) fecha. O cartão nunca entra.
    Avaliacao avaliacao = avaliar("1000.00", Categoria.DEVO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.falta()).isEqualByComparingTo("900");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(sugestao.opcao()).isEqualTo(Opcao.OPCAO_1);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, BOLA, ITEM_CASA, VIAGEM, MANUTENCAO);
    assertThat(sugestao.cobre()).isEqualByComparingTo("900");
    assertThat(avaliacao.listaCompleta()).isEmpty();
  }

  @Test
  @DisplayName("Sem nenhuma opção que cubra, lista completa de elegíveis e quanto ainda falta (R7)")
  void compraDevoFazerSemOpcaoQueCubraTrazListaCompletaEQuantoAindaFaltaria() {
    // falta 1500; elegíveis (sem o cartão) somam 900.
    Avaliacao avaliacao = avaliar("1600.00", Categoria.DEVO_FAZER);

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.sugestoes()).isEmpty();
    assertThat(avaliacao.listaCompleta().stream().map(Item::nome))
        .containsExactly(JANTAR, BOLA, ITEM_CASA, VIAGEM, MANUTENCAO);
    assertThat(avaliacao.aindaFaltaria()).isEqualByComparingTo("600");
  }

  @Test
  @DisplayName(
      "Item parcial já gasto além do previsto continua comprometido, mas não entra nos cortes (R6)")
  void itemParcialGastoAlemDoPrevistoNaoApareceNaSugestaoDeCorte() {
    // Viagem estimada em 300 e já gasta em 350: reserva 350 e cortar não libera nada.
    Mes mes = comViagem(viagem(StatusItem.PARCIAL, "350"));
    assertThat(mes.livre()).isEqualByComparingTo("50");

    // falta 950; os itens que liberam valor somam 600 (com a manutenção), então sai a lista
    // completa.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("1000.00"), Categoria.QUERO_FAZER));

    assertThat(avaliacao.decisao()).isEqualTo(Decisao.CABE_COM_CORTES);
    assertThat(avaliacao.listaCompleta().stream().map(Item::nome))
        .containsExactly(JANTAR, BOLA, ITEM_CASA, MANUTENCAO);
    assertThat(avaliacao.aindaFaltaria()).isEqualByComparingTo("350");
  }

  @Test
  @DisplayName(
      "Dentro do grupo, vence a combinação de menor soma de valores, antes da intensidade (R6)")
  void dentroDoGrupoMenorSomaDeValoresVenceMenorSomaDeIntensidades() {
    Mes mes =
        mesCom(
            "800",
            pendente("item-1", "Curso", Categoria.SERIA_LEGAL, 1, "500"),
            pendente("item-2", "Cinema", Categoria.SERIA_LEGAL, 2, "100"),
            pendente("item-3", "Livro", Categoria.SERIA_LEGAL, 2, "100"));

    // livre 100; falta 200. O curso sozinho corta 500; cinema e livro cortam 200.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("300.00"), Categoria.SERIA_LEGAL));

    assertThat(avaliacao.falta()).isEqualByComparingTo("200");
    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-2", "item-3");
    assertThat(avaliacao.sugestoes().get(0).cobre()).isEqualByComparingTo("200");
  }

  @Test
  @DisplayName(
      "Combinações de mesmo valor no grupo são desempatadas pela menor soma de intensidades (R6)")
  void combinacoesDeMesmoValorSaoDesempatadasPelaMenorSomaDeIntensidades() {
    Mes mes =
        mesCom(
            "300",
            pendente("item-1", "Cinema", Categoria.SERIA_LEGAL, 4, "100"),
            pendente("item-2", "Livro", Categoria.SERIA_LEGAL, 1, "100"));

    // livre 100; falta 50. Os dois cortam 100; o livro tem menor intensidade.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("150.00"), Categoria.SERIA_LEGAL));

    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-2");
  }

  @Test
  @DisplayName(
      "Entre itens EM_DISCUSSAO, entra a combinação de menor valor que cobre o necessário (R6)")
  void entreItensEmDiscussaoEntraACombinacaoDeMenorValorQueCobre() {
    Mes mes =
        mesCom(
            "200",
            emDiscussao("item-1", "Curso", Categoria.SERIA_LEGAL, 1, "100", "marido"),
            emDiscussao("item-2", "Livro", Categoria.QUERO_FAZER, 3, "50", "marido"));

    // livre 50; falta 40. O livro (50) cobre com menos corte que o curso (100).
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("90.00"), Categoria.QUERO_FAZER));

    assertThat(avaliacao.falta()).isEqualByComparingTo("40");
    assertThat(avaliacao.sugestoes()).hasSize(1);
    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-2");
  }

  @Test
  @DisplayName(
      "Compra DEVO_FAZER que a regra de corte cobre corta do QUERO_FAZER só o mínimo, sem opções (R7)")
  void compraDevoFazerCobertaPelaRegraDeCorteTemUmaSugestaoPadrao() {
    // falta 300; os SERIA_LEGAL somam 200; do QUERO_FAZER basta o item para casa.
    Avaliacao avaliacao = avaliar("400.00", Categoria.DEVO_FAZER);

    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(sugestao.opcao()).isEqualTo(Opcao.PADRAO);
    assertThat(nomes(sugestao)).containsExactly(JANTAR, ITEM_CASA);
    assertThat(sugestao.cobre()).isEqualByComparingTo("320");
  }

  @Test
  @DisplayName(
      "Opções 1 e 3 escolhem o DEVO_FAZER de menor intensidade; opções com os mesmos itens valem uma só (R7)")
  void opcoesEscolhemODevoFazerDeMenorIntensidadeENaoSeRepetem() {
    Mes mes =
        mesCom(
            "2000",
            pendente("item-1", "Jantar", Categoria.SERIA_LEGAL, 2, "100"),
            pendente("item-2", "Conserto", Categoria.DEVO_FAZER, 5, "650"),
            pendente("item-3", "Seguro", Categoria.DEVO_FAZER, 1, "700"));

    // livre 550; falta 600; depois do jantar faltam 500. Os dois DEVO_FAZER cobrem sozinhos; vence
    // o seguro, de menor intensidade, mesmo cortando mais. As três opções dão os mesmos itens.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("1150.00"), Categoria.DEVO_FAZER));

    assertThat(avaliacao.sugestoes()).hasSize(1);
    Sugestao sugestao = avaliacao.sugestoes().get(0);
    assertThat(sugestao.opcao()).isEqualTo(Opcao.OPCAO_1);
    assertThat(ids(sugestao)).containsExactly("item-1", "item-3");
  }

  @Test
  @DisplayName(
      "Entre DEVO_FAZER de mesma intensidade, as opções 1 e 3 escolhem o de menor cortável (R7)")
  void entreDevoFazerDeMesmaIntensidadeVenceOMenorCortavel() {
    Mes mes =
        mesCom(
            "2000",
            pendente("item-1", "Jantar", Categoria.SERIA_LEGAL, 2, "100"),
            pendente("item-2", "Seguro", Categoria.DEVO_FAZER, 3, "700"),
            pendente("item-3", "Conserto", Categoria.DEVO_FAZER, 3, "650"));

    // livre 550; falta 600; depois do jantar faltam 500.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("1150.00"), Categoria.DEVO_FAZER));

    assertThat(avaliacao.sugestoes()).hasSize(1);
    assertThat(ids(avaliacao.sugestoes().get(0))).containsExactly("item-1", "item-3");
  }

  @Test
  @DisplayName(
      "Lista completa: em discussão primeiro, depois seria legal, quero e devo, por intensidade e id (R7)")
  void listaCompletaSegueAOrdemUnicaDeCategoriaIntensidadeEId() {
    Mes mes = comExtras(bolsa());

    // livre 10; falta 1490; os elegíveis (sem o cartão) somam 990.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("1500.00"), Categoria.DEVO_FAZER));

    assertThat(avaliacao.sugestoes()).isEmpty();
    assertThat(avaliacao.listaCompleta().stream().map(Item::nome))
        .containsExactly(BOLSA, JANTAR, BOLA, ITEM_CASA, VIAGEM, MANUTENCAO);
    assertThat(avaliacao.aindaFaltaria()).isEqualByComparingTo("500");
  }

  @Test
  @DisplayName("Compra DEVO_FAZER com três opções diferentes as mostra na ordem 1, 2, 3 (R7)")
  void tresOpcoesDiferentesAparecemNaOrdemUmDoisTres() {
    Mes mes =
        mesCom(
            "1300",
            pendente("item-1", "Jantar", Categoria.SERIA_LEGAL, 2, "100"),
            pendente("item-2", "Passeio", Categoria.QUERO_FAZER, 1, "200"),
            pendente("item-3", "Show", Categoria.QUERO_FAZER, 5, "100"),
            pendente("item-4", "Conserto", Categoria.DEVO_FAZER, 2, "300"),
            pendente("item-5", "Seguro", Categoria.DEVO_FAZER, 4, "500"));

    // livre 100; falta 600; a regra de corte (400) não cobre. Depois do jantar faltam 500.
    // Opção 1: os dois QUERO (300) e o conserto, que cobre os 200 restantes.
    // Opção 2: passeio e conserto, soma de intensidades 3 contra 4 do seguro sozinho.
    // Opção 3: o seguro, único DEVO que cobre 500 sozinho.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("700.00"), Categoria.DEVO_FAZER));

    assertThat(avaliacao.sugestoes()).hasSize(3);
    assertThat(avaliacao.sugestoes().stream().map(Sugestao::opcao))
        .containsExactly(Opcao.OPCAO_1, Opcao.OPCAO_2, Opcao.OPCAO_3);
    assertThat(ids(avaliacao.sugestoes().get(0)))
        .containsExactly("item-1", "item-2", "item-3", "item-4");
    assertThat(ids(avaliacao.sugestoes().get(1))).containsExactly("item-1", "item-2", "item-4");
    assertThat(ids(avaliacao.sugestoes().get(2))).containsExactly("item-1", "item-5");
  }

  @Test
  @DisplayName(
      "Opção 2: com a mesma soma de intensidades, vence a combinação de menor soma de cortáveis (R7)")
  void opcaoDoisDesempataPelaMenorSomaDeCortaveis() {
    Mes mes =
        mesCom(
            "1820",
            pendente("item-1", "Jantar", Categoria.SERIA_LEGAL, 2, "100"),
            pendente("item-2", "Seguro", Categoria.DEVO_FAZER, 2, "600"),
            pendente("item-3", "Dentista", Categoria.DEVO_FAZER, 1, "260"),
            pendente("item-4", "Óculos", Categoria.DEVO_FAZER, 1, "260"),
            pendente("item-5", "Conserto", Categoria.DEVO_FAZER, 5, "500"));

    // livre 100; falta 600; depois do jantar faltam 500. Seguro sozinho e dentista com óculos
    // somam intensidade 2; dentista e óculos cortam menos (520 contra 600). O conserto corta
    // 500, mas tem intensidade 5. Pelo id, o seguro (item-2) venceria.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("700.00"), Categoria.DEVO_FAZER));

    assertThat(avaliacao.sugestoes())
        .filteredOn(sugestao -> sugestao.opcao() == Opcao.OPCAO_2)
        .singleElement()
        .satisfies(
            sugestao -> assertThat(ids(sugestao)).containsExactly("item-1", "item-3", "item-4"));
  }

  @Test
  @DisplayName(
      "Opção 2: com a mesma soma de intensidades e de cortáveis, vence a combinação de menor id (R7)")
  void opcaoDoisDesempataPeloMenorId() {
    Mes mes =
        mesCom(
            "1050",
            pendente("item-1", "Jantar", Categoria.SERIA_LEGAL, 2, "100"),
            pendente("item-6", "Passeio", Categoria.QUERO_FAZER, 1, "200"),
            pendente("item-7", "Show", Categoria.QUERO_FAZER, 5, "50"),
            pendente("item-3", "Dentista", Categoria.DEVO_FAZER, 1, "300"),
            pendente("item-4", "Óculos", Categoria.DEVO_FAZER, 1, "300"));

    // livre 100; falta 600; a regra de corte (350) não cobre. Depois do jantar faltam 500.
    // Passeio com dentista e passeio com óculos empatam: intensidade 2 e cortável 500. Vence o
    // dentista (item-3), pelo menor id. Nenhum DEVO cobre 500 sozinho, então não há opção 3.
    Avaliacao avaliacao = mes.avaliar(new Compra(valor("700.00"), Categoria.DEVO_FAZER));

    assertThat(avaliacao.sugestoes()).hasSize(2);
    assertThat(avaliacao.sugestoes().get(0).opcao()).isEqualTo(Opcao.OPCAO_1);
    assertThat(ids(avaliacao.sugestoes().get(0)))
        .containsExactly("item-1", "item-6", "item-7", "item-3");
    assertThat(avaliacao.sugestoes().get(1).opcao()).isEqualTo(Opcao.OPCAO_2);
    assertThat(ids(avaliacao.sugestoes().get(1))).containsExactly("item-1", "item-6", "item-3");
  }
}
