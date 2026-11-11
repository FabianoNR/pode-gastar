package br.com.fabianonr.podegastar.aplicacao.porta.saida;

import br.com.fabianonr.podegastar.dominio.Mes;
import java.util.Optional;

/** Porta de saída: de onde vem o mês guardado no servidor. */
public interface MesRepositorio {

  Optional<Mes> buscarPorId(String mesId);
}
