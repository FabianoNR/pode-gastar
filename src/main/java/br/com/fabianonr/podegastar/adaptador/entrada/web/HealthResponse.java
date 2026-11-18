package br.com.fabianonr.podegastar.adaptador.entrada.web;

/** Corpo do /health: status e commit no ar (spec, O1). */
public record HealthResponse(String status, String commit) {}
