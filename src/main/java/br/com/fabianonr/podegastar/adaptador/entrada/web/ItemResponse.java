package br.com.fabianonr.podegastar.adaptador.entrada.web;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemResponse(
    String id, String nome, String status, String decisorResponsavel, String mensagem) {}
