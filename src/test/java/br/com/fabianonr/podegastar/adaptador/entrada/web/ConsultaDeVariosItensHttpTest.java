package br.com.fabianonr.podegastar.adaptador.entrada.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fabianonr.podegastar.configuracao.ConfiguracaoAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Consulta de vários itens em POST /meses/{mesId}/avaliacoes, sobre o mês de exemplo. */
@Import(ConfiguracaoAplicacao.class)
@WebMvcTest(controllers = {AvaliacoesController.class, AvaliacaoController.class})
class ConsultaDeVariosItensHttpTest {

  private static final String MES_EXEMPLO = "mes-exemplo";
  private static final String ITEM_VALIDO =
      "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\"}";

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper leitorDeJson = new ObjectMapper();

  private ResultActions consultar(String mesId, String corpo) throws Exception {
    return mockMvc.perform(
        post("/meses/{mesId}/avaliacoes", mesId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  private ResultActions consultarNoMesExemplo(String corpo) throws Exception {
    return consultar(MES_EXEMPLO, corpo);
  }

  private static String listaCom(String... itens) {
    return "{\"itens\": [" + String.join(", ", itens) + "]}";
  }

  private static String listaComItensValidos(int quantidade) {
    return listaCom(Collections.nCopies(quantidade, ITEM_VALIDO).toArray(String[]::new));
  }

  @Test
  @DisplayName(
      "Três itens voltam avaliados como no GET, da maior intensidade para a menor, com o eco (CA74)")
  void avaliaTresItensNaOrdemDeIntensidadeComOEco() throws Exception {
    consultarNoMesExemplo(
            listaCom(
                "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\"}",
                "{\"nome\": \"Tênis\", \"valor\": 300.00, \"categoria\": \"QUERO_FAZER\","
                    + " \"intensidade\": 5}",
                "{\"nome\": \"Viagem extra\", \"valor\": 1700.00, \"categoria\": \"QUERO_FAZER\","
                    + " \"intensidade\": 3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avisos", hasSize(0)))
        .andExpect(
            jsonPath("$.avaliacoes[*].nome").value(contains("Tênis", "Viagem extra", "Livro")))
        .andExpect(jsonPath("$.avaliacoes[0].valor").value("300.00"))
        .andExpect(jsonPath("$.avaliacoes[0].categoria").value("QUERO_FAZER"))
        .andExpect(jsonPath("$.avaliacoes[0].intensidade").value(5))
        .andExpect(jsonPath("$.avaliacoes[0].decisao").value("CABE_COM_CORTES"))
        .andExpect(jsonPath("$.avaliacoes[0].falta").value("200.00"))
        .andExpect(
            jsonPath("$.avaliacoes[0].sugestoes[0].itens[*].nome")
                .value(contains("Jantar fora no mês", "Bola de futebol")))
        .andExpect(jsonPath("$.avaliacoes[0].itensMaisDesejadosQueACompra", hasSize(0)))
        .andExpect(jsonPath("$.avaliacoes[0].avisos", hasSize(0)))
        .andExpect(jsonPath("$.avaliacoes[1].valor").value("1700.00"))
        .andExpect(jsonPath("$.avaliacoes[1].categoria").value("QUERO_FAZER"))
        .andExpect(jsonPath("$.avaliacoes[1].intensidade").value(3))
        .andExpect(jsonPath("$.avaliacoes[1].decisao").value("NAO_CABE"))
        .andExpect(jsonPath("$.avaliacoes[1].falta").value("100.00"))
        .andExpect(
            jsonPath("$.avaliacoes[1].motivo")
                .value("compra excede o total que ainda sobra no mês"))
        .andExpect(jsonPath("$.avaliacoes[2].valor").value("80.00"))
        .andExpect(jsonPath("$.avaliacoes[2].categoria").value("QUERO_FAZER"))
        .andExpect(jsonPath("$.avaliacoes[2].intensidade").value(1))
        .andExpect(jsonPath("$.avaliacoes[2].decisao").value("APROVADO"))
        .andExpect(jsonPath("$.avaliacoes[2].livre").value("100.00"))
        .andExpect(jsonPath("$.avaliacoes[2].saldoRestante").value("20.00"));
  }

  @Test
  @DisplayName(
      "Cada avaliação da lista é a mesma do GET para o mesmo valor, categoria e intensidade (CA74)")
  void avaliacaoDaListaEIgualADoGet() throws Exception {
    String corpoDaLista =
        consultarNoMesExemplo(
                listaCom(
                    "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\"}",
                    "{\"nome\": \"Tênis\", \"valor\": 300.00, \"categoria\": \"QUERO_FAZER\","
                        + " \"intensidade\": 5}",
                    "{\"nome\": \"Viagem extra\", \"valor\": 1700.00, \"categoria\":"
                        + " \"QUERO_FAZER\", \"intensidade\": 3}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode avaliacoes = leitorDeJson.readTree(corpoDaLista).get("avaliacoes");

    assertThat(semEco(avaliacoes.get(0))).isEqualTo(respostaDoGet("300.00", "5"));
    assertThat(semEco(avaliacoes.get(1))).isEqualTo(respostaDoGet("1700.00", "3"));
    assertThat(semEco(avaliacoes.get(2))).isEqualTo(respostaDoGet("80.00", null));
  }

  private JsonNode respostaDoGet(String valor, String intensidade) throws Exception {
    var consulta =
        get("/meses/{mesId}/avaliacao", MES_EXEMPLO)
            .param("valor", valor)
            .param("categoria", "QUERO_FAZER");
    if (intensidade != null) {
      consulta = consulta.param("intensidade", intensidade);
    }
    return leitorDeJson.readTree(
        mockMvc.perform(consulta).andReturn().getResponse().getContentAsString());
  }

  private static JsonNode semEco(JsonNode avaliacao) {
    ObjectNode copia = avaliacao.deepCopy();
    copia.remove(List.of("nome", "valor", "categoria", "intensidade"));
    return copia;
  }

  @Test
  @DisplayName("Intensidades empatadas saem na ordem do pedido, e a sem intensidade vale 1 (CA75)")
  void empateDeIntensidadeSaiNaOrdemDoPedido() throws Exception {
    consultarNoMesExemplo(
            listaCom(
                "{\"nome\": \"C\", \"valor\": 50.00, \"categoria\": \"QUERO_FAZER\"}",
                "{\"nome\": \"A\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\","
                    + " \"intensidade\": 2}",
                "{\"nome\": \"B\", \"valor\": 150.00, \"categoria\": \"SERIA_LEGAL\","
                    + " \"intensidade\": 2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avaliacoes[*].nome").value(contains("A", "B", "C")))
        .andExpect(jsonPath("$.avaliacoes[0].decisao").value("APROVADO"))
        .andExpect(jsonPath("$.avaliacoes[0].saldoRestante").value("20.00"))
        .andExpect(jsonPath("$.avaliacoes[1].decisao").value("CABE_COM_CORTES"))
        .andExpect(jsonPath("$.avaliacoes[1].falta").value("50.00"))
        .andExpect(
            jsonPath("$.avaliacoes[1].sugestoes[0].itens[*].nome")
                .value(contains("Bola de futebol")))
        .andExpect(jsonPath("$.avaliacoes[2].decisao").value("APROVADO"))
        .andExpect(jsonPath("$.avaliacoes[2].saldoRestante").value("50.00"))
        .andExpect(jsonPath("$.avaliacoes[2].intensidade").value(1));
  }

  @Test
  @DisplayName("O aviso de intensidade vem só na avaliação que o gerou, e o topo vem vazio (CA76)")
  void avisoDeIntensidadeVemSoNaPropriaAvaliacao() throws Exception {
    consultarNoMesExemplo(
            listaCom(
                "{\"nome\": \"X\", \"valor\": 300.00, \"categoria\": \"QUERO_FAZER\"}",
                "{\"nome\": \"Y\", \"valor\": 300.00, \"categoria\": \"QUERO_FAZER\","
                    + " \"intensidade\": 5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avisos", hasSize(0)))
        .andExpect(jsonPath("$.avaliacoes[*].nome").value(contains("Y", "X")))
        .andExpect(jsonPath("$.avaliacoes[0].avisos", hasSize(0)))
        .andExpect(
            jsonPath("$.avaliacoes[1].avisos")
                .value(
                    contains(
                        "A intensidade da compra não foi informada e foi considerada 1."
                            + " A compra é menos desejada que os itens a cortar:"
                            + " \"Jantar fora no mês\", \"Bola de futebol\".")));
  }

  @Test
  @DisplayName("Dois itens com o mesmo nome no pedido são avaliados sem aviso (CA78)")
  void nomeRepetidoNoPedidoNaoGeraAviso() throws Exception {
    consultarNoMesExemplo(listaCom(ITEM_VALIDO, ITEM_VALIDO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avisos", hasSize(0)))
        .andExpect(jsonPath("$.avaliacoes[*].decisao").value(contains("APROVADO", "APROVADO")))
        .andExpect(jsonPath("$.avaliacoes[0].avisos", hasSize(0)))
        .andExpect(jsonPath("$.avaliacoes[1].avisos", hasSize(0)));
  }

  @Test
  @DisplayName("O nome volta como enviado, sem cortar espaços, e o valor com duas casas (CA74)")
  void ecoDoNomeComoEnviadoEDoValorComDuasCasas() throws Exception {
    consultarNoMesExemplo(
            listaCom("{\"nome\": \" Livro \", \"valor\": 80, \"categoria\": \"QUERO_FAZER\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avaliacoes[0].nome").value(" Livro "))
        .andExpect(jsonPath("$.avaliacoes[0].valor").value("80.00"));
  }

  @Test
  @DisplayName("Lista com 20 itens responde 200 com 20 avaliações (CA79)")
  void vinteItensRespondem200() throws Exception {
    consultarNoMesExemplo(listaComItensValidos(20))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avaliacoes", hasSize(20)));
  }

  @Test
  @DisplayName("Campos a mais no item são ignorados (CA80)")
  void camposAMaisNoItemSaoIgnorados() throws Exception {
    consultarNoMesExemplo(
            listaCom(
                "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\","
                    + " \"loja\": \"centro\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avaliacoes[0].decisao").value("APROVADO"))
        .andExpect(jsonPath("$.avaliacoes[0].loja").doesNotExist());
  }

  static Stream<Arguments> listasInvalidas() {
    return Stream.of(
        Arguments.of("corpo vazio", "", "itens é obrigatório"),
        Arguments.of("sem itens", "{}", "itens é obrigatório"),
        Arguments.of("itens nulo", "{\"itens\": null}", "itens é obrigatório"),
        Arguments.of("lista vazia", "{\"itens\": []}", "itens deve ter de 1 a 20 itens"),
        Arguments.of("21 itens", listaComItensValidos(21), "itens deve ter de 1 a 20 itens"),
        Arguments.of("itens não é lista", "{\"itens\": \"Livro\"}", "valor inválido para itens"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("listasInvalidas")
  @DisplayName(
      "Lista ausente, vazia, com mais de 20 itens ou que não é lista dá 400 em itens (CA79)")
  void listaInvalidaDa400ApontandoItens(String caso, String corpo, String mensagem)
      throws Exception {
    consultarNoMesExemplo(corpo)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campo").value("itens"))
        .andExpect(jsonPath("$.mensagem").value(mensagem))
        .andExpect(jsonPath("$.avaliacoes").doesNotExist());
  }

  @Test
  @DisplayName("Corpo que não é JSON válido dá 400 sem campo (CA79)")
  void corpoQueNaoEJsonDa400SemCampo() throws Exception {
    consultarNoMesExemplo("{\"itens\": [")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campo").doesNotExist())
        .andExpect(jsonPath("$.mensagem").value("corpo da requisição inválido"));
  }

  @Test
  @DisplayName("Item nulo na lista dá 400 apontando a posição do item (CA79)")
  void itemNuloDa400ApontandoAPosicao() throws Exception {
    consultarNoMesExemplo(listaCom(ITEM_VALIDO, "null"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campo").value("itens[1]"))
        .andExpect(jsonPath("$.mensagem").value("item é obrigatório"));
  }

  static Stream<Arguments> itensInvalidos() {
    return Stream.of(
        Arguments.of(
            "nome ausente",
            "{\"valor\": 80.00, \"categoria\": \"QUERO_FAZER\"}",
            "itens[1].nome",
            "nome é obrigatório"),
        Arguments.of(
            "nome vazio",
            "{\"nome\": \"\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\"}",
            "itens[1].nome",
            "nome é obrigatório"),
        Arguments.of(
            "nome só com espaços",
            "{\"nome\": \"   \", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\"}",
            "itens[1].nome",
            "nome é obrigatório"),
        Arguments.of(
            "nome que não é texto",
            "{\"nome\": 123, \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\"}",
            "itens[1].nome",
            "valor inválido para nome"),
        Arguments.of(
            "valor zero",
            "{\"nome\": \"Livro\", \"valor\": 0, \"categoria\": \"QUERO_FAZER\"}",
            "itens[1].valor",
            "valor deve ser maior que zero"),
        Arguments.of(
            "valor negativo",
            "{\"nome\": \"Livro\", \"valor\": -10.00, \"categoria\": \"QUERO_FAZER\"}",
            "itens[1].valor",
            "valor deve ser maior que zero"),
        Arguments.of(
            "valor com três casas",
            "{\"nome\": \"Livro\", \"valor\": 80.001, \"categoria\": \"QUERO_FAZER\"}",
            "itens[1].valor",
            "valor deve ter no máximo duas casas decimais"),
        Arguments.of(
            "valor não numérico",
            "{\"nome\": \"Livro\", \"valor\": \"abc\", \"categoria\": \"QUERO_FAZER\"}",
            "itens[1].valor",
            "valor inválido para valor"),
        Arguments.of(
            "valor numérico como texto",
            "{\"nome\": \"Livro\", \"valor\": \"80.00\", \"categoria\": \"QUERO_FAZER\"}",
            "itens[1].valor",
            "valor inválido para valor"),
        Arguments.of(
            "valor ausente",
            "{\"nome\": \"Livro\", \"categoria\": \"QUERO_FAZER\"}",
            "itens[1].valor",
            "valor é obrigatório"),
        Arguments.of(
            "categoria ausente",
            "{\"nome\": \"Livro\", \"valor\": 80.00}",
            "itens[1].categoria",
            "categoria é obrigatório"),
        Arguments.of(
            "categoria fora do conjunto",
            "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"XYZ\"}",
            "itens[1].categoria",
            "valor inválido para categoria"),
        Arguments.of(
            "intensidade 0",
            "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\","
                + " \"intensidade\": 0}",
            "itens[1].intensidade",
            "intensidade deve ser um inteiro de 1 a 5"),
        Arguments.of(
            "intensidade 6",
            "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\","
                + " \"intensidade\": 6}",
            "itens[1].intensidade",
            "intensidade deve ser um inteiro de 1 a 5"),
        Arguments.of(
            "intensidade 2.5",
            "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\","
                + " \"intensidade\": 2.5}",
            "itens[1].intensidade",
            "valor inválido para intensidade"),
        Arguments.of(
            "intensidade 2.0",
            "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\","
                + " \"intensidade\": 2.0}",
            "itens[1].intensidade",
            "valor inválido para intensidade"),
        Arguments.of(
            "intensidade não numérica",
            "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\","
                + " \"intensidade\": \"abc\"}",
            "itens[1].intensidade",
            "valor inválido para intensidade"),
        Arguments.of(
            "intensidade numérica como texto",
            "{\"nome\": \"Livro\", \"valor\": 80.00, \"categoria\": \"QUERO_FAZER\","
                + " \"intensidade\": \"3\"}",
            "itens[1].intensidade",
            "valor inválido para intensidade"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("itensInvalidos")
  @DisplayName(
      "Item inválido derruba a lista com 400 apontando o campo pela posição do item (CA80)")
  void itemInvalidoDa400ApontandoOCampoDoItem(
      String caso, String itemInvalido, String campo, String mensagem) throws Exception {
    consultarNoMesExemplo(listaCom(ITEM_VALIDO, itemInvalido))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campo").value(campo))
        .andExpect(jsonPath("$.mensagem").value(mensagem))
        .andExpect(jsonPath("$.avaliacoes").doesNotExist());
  }

  @Test
  @DisplayName("Com mais de um erro vale o primeiro item com erro, e o 400 vem antes do 404 (CA80)")
  void primeiroItemComErroVemAntesDoMesInexistente() throws Exception {
    consultar(
            "nao-existe",
            listaCom(
                "{\"valor\": 80.00, \"categoria\": \"QUERO_FAZER\"}",
                ITEM_VALIDO,
                "{\"nome\": \"Livro\", \"valor\": 0, \"categoria\": \"QUERO_FAZER\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campo").value("itens[0].nome"))
        .andExpect(jsonPath("$.mensagem").value("nome é obrigatório"));
  }

  @Test
  @DisplayName("Dentro do item, o nome é conferido antes do valor (CA80)")
  void nomeEConferidoAntesDoValor() throws Exception {
    consultarNoMesExemplo(
            listaCom("{\"nome\": \"\", \"valor\": 0, \"categoria\": \"QUERO_FAZER\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campo").value("itens[0].nome"));
  }

  @Test
  @DisplayName("Mês inexistente dá 404 no formato do GET (CA81)")
  void mesInexistenteDa404() throws Exception {
    consultar("nao-existe", listaCom(ITEM_VALIDO))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.campo").doesNotExist())
        .andExpect(jsonPath("$.mensagem").value("mês não encontrado: nao-existe"))
        .andExpect(jsonPath("$.avaliacoes").doesNotExist());
  }
}
