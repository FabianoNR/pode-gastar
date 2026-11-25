package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.aplicacao.porta.entrada.CompraNomeada;
import br.com.fabianonr.podegastar.dominio.Categoria;
import br.com.fabianonr.podegastar.dominio.Compra;
import br.com.fabianonr.podegastar.dominio.ValorInvalidoException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.NumberUtils;

/**
 * Lê e valida o corpo da consulta de vários itens, à mão, sem a conversão automática de tipos do
 * Jackson nos campos do item. Aponta só o primeiro erro: o primeiro item com erro e, dentro dele, a
 * ordem nome, valor, categoria, intensidade. As mensagens de cada campo são as do GET.
 */
final class LeitorDoPedidoDeCompras {

  private static final int MAXIMO_DE_ITENS = 20;
  private static final String CAMPO_ITENS = "itens";

  private static final JsonMapper LEITOR_DE_JSON =
      JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

  private LeitorDoPedidoDeCompras() {}

  static List<CompraNomeada> ler(String corpo) {
    JsonNode itens = listaDeItens(corpoEmJson(corpo));
    List<CompraNomeada> compras = new ArrayList<>();
    for (int posicao = 0; posicao < itens.size(); posicao++) {
      compras.add(item(itens.get(posicao), CAMPO_ITENS + "[" + posicao + "]"));
    }
    return compras;
  }

  private static JsonNode corpoEmJson(String corpo) {
    if (corpo == null || corpo.isBlank()) {
      throw obrigatorio(CAMPO_ITENS);
    }
    try {
      return LEITOR_DE_JSON.readTree(corpo);
    } catch (JsonProcessingException excecao) {
      throw new ValorInvalidoException(null, "corpo da requisição inválido");
    }
  }

  private static JsonNode listaDeItens(JsonNode corpo) {
    JsonNode itens = corpo.get(CAMPO_ITENS);
    if (ausente(itens)) {
      throw obrigatorio(CAMPO_ITENS);
    }
    if (!itens.isArray()) {
      throw tipoInvalido(CAMPO_ITENS, CAMPO_ITENS);
    }
    if (itens.isEmpty() || itens.size() > MAXIMO_DE_ITENS) {
      throw new ValorInvalidoException(CAMPO_ITENS, "itens deve ter de 1 a 20 itens");
    }
    return itens;
  }

  private static CompraNomeada item(JsonNode item, String campoDoItem) {
    if (ausente(item)) {
      throw new ValorInvalidoException(campoDoItem, "item é obrigatório");
    }
    String prefixo = campoDoItem + ".";
    String nome = nome(item.get("nome"), prefixo);
    BigDecimal valor = valor(item.get("valor"), prefixo);
    Categoria categoria = categoria(item.get("categoria"), prefixo);
    Integer intensidade = intensidade(item.get("intensidade"), prefixo);
    Compra compra =
        intensidade == null
            ? new Compra(valor, categoria)
            : new Compra(valor, categoria, intensidade);
    return new CompraNomeada(nome, compra);
  }

  private static String nome(JsonNode nome, String prefixo) {
    if (ausente(nome)) {
      throw obrigatorio(prefixo, "nome");
    }
    if (!nome.isTextual()) {
      throw tipoInvalido(prefixo + "nome", "nome");
    }
    if (nome.textValue().isBlank()) {
      throw obrigatorio(prefixo, "nome");
    }
    return nome.textValue();
  }

  /**
   * O valor vem como texto (ADR 0002) e é convertido como o GET converte {@code ?valor=}: texto
   * vazio é ausente, e os espaços são ignorados.
   */
  private static BigDecimal valor(JsonNode valor, String prefixo) {
    if (ausente(valor) || (valor.isTextual() && valor.textValue().isBlank())) {
      throw obrigatorio(prefixo, "valor");
    }
    if (!valor.isTextual()) {
      throw tipoInvalido(prefixo + "valor", "valor");
    }
    BigDecimal valorDecimal = valorDecimal(valor.textValue(), prefixo);
    CasasDecimaisDoValor.exigirNoMaximoDuas(valorDecimal, prefixo + "valor");
    comCampoDoItem(prefixo, () -> Compra.exigirValorPositivo(valorDecimal));
    return valorDecimal;
  }

  private static BigDecimal valorDecimal(String valor, String prefixo) {
    try {
      return NumberUtils.parseNumber(valor, BigDecimal.class);
    } catch (IllegalArgumentException excecao) {
      throw tipoInvalido(prefixo + "valor", "valor");
    }
  }

  /** Como no GET, o texto é comparado sem os espaços das pontas. */
  private static Categoria categoria(JsonNode categoria, String prefixo) {
    if (ausente(categoria)) {
      throw obrigatorio(prefixo, "categoria");
    }
    if (!categoria.isTextual()) {
      throw tipoInvalido(prefixo + "categoria", "categoria");
    }
    try {
      return Categoria.valueOf(categoria.textValue().trim());
    } catch (IllegalArgumentException excecao) {
      throw tipoInvalido(prefixo + "categoria", "categoria");
    }
  }

  /** Nada quando a intensidade não foi informada: a compra vale 1. */
  private static Integer intensidade(JsonNode intensidade, String prefixo) {
    if (ausente(intensidade)) {
      return null;
    }
    if (!intensidade.isIntegralNumber() || !intensidade.canConvertToInt()) {
      throw tipoInvalido(prefixo + "intensidade", "intensidade");
    }
    int intensidadeInteira = intensidade.intValue();
    comCampoDoItem(prefixo, () -> Compra.exigirIntensidadeDeUmACinco(intensidadeInteira));
    return intensidadeInteira;
  }

  /** Campo ausente ou com {@code null} no JSON. */
  private static boolean ausente(JsonNode campo) {
    return campo == null || campo.isNull();
  }

  /** Repete a validação do domínio, apontando o campo dentro do item. */
  private static void comCampoDoItem(String prefixo, Runnable validacaoDoDominio) {
    try {
      validacaoDoDominio.run();
    } catch (ValorInvalidoException excecao) {
      throw new ValorInvalidoException(prefixo + excecao.campo(), excecao.getMessage());
    }
  }

  private static ValorInvalidoException obrigatorio(String campo) {
    return new ValorInvalidoException(campo, campo + " é obrigatório");
  }

  private static ValorInvalidoException obrigatorio(String prefixo, String nomeDoCampo) {
    return new ValorInvalidoException(prefixo + nomeDoCampo, nomeDoCampo + " é obrigatório");
  }

  private static ValorInvalidoException tipoInvalido(String campo, String nomeDoCampo) {
    return new ValorInvalidoException(campo, "valor inválido para " + nomeDoCampo);
  }
}
