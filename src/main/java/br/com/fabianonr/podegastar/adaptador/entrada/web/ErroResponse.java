package br.com.fabianonr.podegastar.adaptador.entrada.web;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Corpo dos erros 400 e 404. O campo aponta o parâmetro inválido (contrato da spec). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(String campo, String mensagem) {}
