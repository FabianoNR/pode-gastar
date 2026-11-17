package br.com.fabianonr.podegastar.operacao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Roda os scripts de operação em um processo bash com ambiente limpo: só PATH, HOME, as variáveis
 * passadas pelo teste e o intervalo e o prazo reduzidos (O13).
 */
final class ExecucaoDeScript {

  static final Path PASTA_DOS_SCRIPTS = Path.of("scripts").toAbsolutePath();
  static final int INTERVALO_DE_TESTE_EM_SEGUNDOS = 1;
  static final int PRAZO_DE_TESTE_EM_SEGUNDOS = 3;

  private static final Duration LIMITE_DO_PROCESSO = Duration.ofSeconds(60);

  private final Map<String, String> ambiente = new HashMap<>();

  private ExecucaoDeScript() {
    ambiente.put("PATH", System.getenv("PATH"));
    ambiente.put("HOME", System.getProperty("java.io.tmpdir"));
    ambiente.put("ESPERA_INTERVALO_SEGUNDOS", String.valueOf(INTERVALO_DE_TESTE_EM_SEGUNDOS));
    ambiente.put("ESPERA_PRAZO_SEGUNDOS", String.valueOf(PRAZO_DE_TESTE_EM_SEGUNDOS));
  }

  static ExecucaoDeScript comAmbienteDeTeste() {
    return new ExecucaoDeScript();
  }

  ExecucaoDeScript comVariavel(String nome, String valor) {
    ambiente.put(nome, valor);
    return this;
  }

  ExecucaoDeScript comIntervaloEmSegundos(int intervalo) {
    return comVariavel("ESPERA_INTERVALO_SEGUNDOS", String.valueOf(intervalo));
  }

  ExecucaoDeScript comPrazoEmSegundos(int prazo) {
    return comVariavel("ESPERA_PRAZO_SEGUNDOS", String.valueOf(prazo));
  }

  /** Roda um script da pasta scripts/ com os argumentos dados. */
  Resultado rodarScript(String nomeDoScript, String... argumentos) {
    var comando =
        new ArrayList<String>(List.of("bash", PASTA_DOS_SCRIPTS.resolve(nomeDoScript).toString()));
    comando.addAll(List.of(argumentos));
    return rodar(comando);
  }

  /** Carrega scripts/operacao.sh e chama uma das funções dele com os argumentos dados. */
  Resultado rodarFuncao(String nomeDaFuncao, String... argumentos) {
    var biblioteca = PASTA_DOS_SCRIPTS.resolve("operacao.sh").toString();
    var comando =
        new ArrayList<String>(
            List.of("bash", "-c", "source \"$0\" && \"$@\"", biblioteca, nomeDaFuncao));
    comando.addAll(List.of(argumentos));
    return rodar(comando);
  }

  private Resultado rodar(List<String> comando) {
    var construtor = new ProcessBuilder(comando).redirectErrorStream(true);
    construtor.environment().clear();
    construtor.environment().putAll(ambiente);
    try {
      var inicio = System.nanoTime();
      var processo = construtor.start();
      var saida = CompletableFuture.supplyAsync(() -> lerSaida(processo));
      if (!processo.waitFor(LIMITE_DO_PROCESSO.toSeconds(), TimeUnit.SECONDS)) {
        processo.destroyForcibly();
        throw new IllegalStateException("O script passou de " + LIMITE_DO_PROCESSO + ".");
      }
      var duracao = Duration.ofNanos(System.nanoTime() - inicio);
      return new Resultado(processo.exitValue(), saida.join(), duracao);
    } catch (IOException erro) {
      throw new UncheckedIOException(erro);
    } catch (InterruptedException erro) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(erro);
    }
  }

  private static String lerSaida(Process processo) {
    try {
      return new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException erro) {
      throw new UncheckedIOException(erro);
    }
  }

  record Resultado(int codigoDeSaida, String saida, Duration duracao) {

    boolean terminouComSucesso() {
      return codigoDeSaida == 0;
    }
  }
}
