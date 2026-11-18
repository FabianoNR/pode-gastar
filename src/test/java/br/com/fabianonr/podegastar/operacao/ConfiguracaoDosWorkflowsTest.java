package br.com.fabianonr.podegastar.operacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/** Configuração dos workflows de deploy normal e de rollback (O5, O11 a O14). */
class ConfiguracaoDosWorkflowsTest {

  private static final Path CI = Path.of(".github/workflows/ci.yml");
  private static final Path ROLLBACK = Path.of(".github/workflows/rollback.yml");
  private static final Path SCRIPT_DE_ROLLBACK = Path.of("scripts/rollback.sh");
  private static final Path SCRIPT_DE_DEPLOY = Path.of("scripts/deploy.sh");
  private static final Path FUNCOES_DA_OPERACAO = Path.of("scripts/operacao.sh");
  private static final Pattern SEGREDO = Pattern.compile("secrets\\.([A-Z_]+)");

  @Test
  @DisplayName("O job de rollback só roda na main (O11)")
  void jobDeRollbackSoRodaNaMain() throws IOException {
    var condicao = String.valueOf(jobDeRollback().get("if"));

    assertThat(condicao.replace(" ", "")).isEqualTo("github.ref=='refs/heads/main'");
  }

  @Test
  @DisplayName("Deploy hook e smoke test formam um único job no ci.yml (O12)")
  void deployHookESmokeTestFormamUmUnicoJob() throws IOException {
    var jobs = jobs(CI);

    assertThat(jobs.keySet()).containsExactlyInAnyOrder("verify", "deploy");
    assertThat(new Yaml().dump(jobs.get("deploy"))).contains("scripts/deploy.sh");
  }

  @Test
  @DisplayName("O job de deploy depende do verify e só roda em push na main (ADR 0014)")
  void jobDeDeployDependeDoVerifyESoRodaEmPushNaMain() throws IOException {
    var jobDeDeploy = mapa(jobs(CI).get("deploy"));
    var condicao = String.valueOf(jobDeDeploy.get("if"));

    assertThat(jobDeDeploy).containsEntry("needs", "verify");
    assertThat(condicao.replace(" ", ""))
        .isEqualTo("github.event_name=='push'&&github.ref=='refs/heads/main'");
  }

  @Test
  @DisplayName("O job de deploy espera o /health expor o SHA do commit publicado (O4, ADR 0014)")
  void jobDeDeployEsperaOShaDoCommitPublicado() throws IOException {
    var jobDeDeploy = new Yaml().dump(jobs(CI).get("deploy"));

    assertThat(jobDeDeploy).contains("${{ github.sha }}").contains("scripts/deploy.sh");
  }

  @Test
  @DisplayName("O ci.yml não publica imagem nem faz login no GHCR (ADR 0014)")
  void ciNaoPublicaImagemNemFazLoginNoGhcr() throws IOException {
    var workflow = Files.readString(CI);

    assertThat(jobs(CI)).doesNotContainKey("publish");
    assertThat(workflow)
        .doesNotContain("ghcr.io")
        .doesNotContain("docker/login-action")
        .doesNotContain("docker/build-push-action")
        .doesNotContain("packages: write")
        .doesNotContain("IMAGE_URL");
  }

  @Test
  @DisplayName(
      "Os scripts de deploy não enviam imgURL ao deploy hook nem recebem imagem (ADR 0014)")
  void scriptsDeDeployNaoEnviamImgUrl() throws IOException {
    for (var script : new Path[] {SCRIPT_DE_DEPLOY, FUNCOES_DA_OPERACAO}) {
      assertThat(Files.readString(script))
          .as(script.toString())
          .doesNotContain("imgURL")
          .doesNotContain("imagem")
          .doesNotContain("ghcr.io");
    }
  }

  @Test
  @DisplayName(
      "Deploy normal e rollback usam o mesmo grupo de concorrência, sem cancelar o que está rodando (O12)")
  void deployNormalERollbackCompartilhamOGrupoDeConcorrencia() throws IOException {
    var concorrenciaDoDeploy = mapa(jobs(CI).get("deploy")).get("concurrency");
    var concorrenciaDoRollback = jobDeRollback().get("concurrency");

    assertThat(concorrenciaDoDeploy).isEqualTo(concorrenciaDoRollback);
    assertThat(mapa(concorrenciaDoRollback)).containsEntry("cancel-in-progress", false);
    assertThat(mapa(concorrenciaDoRollback).get("group")).isNotNull();
  }

  @Test
  @DisplayName("O job de deploy usa só RENDER_DEPLOY_HOOK_URL e SERVICE_URL, sem environment (O14)")
  void jobDeDeployUsaSoOsSegredosDoDeploy() throws IOException {
    var jobDeDeploy = mapa(jobs(CI).get("deploy"));

    assertThat(segredosUsados(jobDeDeploy))
        .containsExactlyInAnyOrder("RENDER_DEPLOY_HOOK_URL", "SERVICE_URL");
    assertThat(jobDeDeploy).doesNotContainKey("environment");
  }

  @Test
  @DisplayName(
      "O job de rollback usa só RENDER_API_KEY, RENDER_SERVICE_ID e SERVICE_URL, sem environment (O14)")
  void jobDeRollbackUsaSoOsSegredosDoRollback() throws IOException {
    var jobDeRollback = jobDeRollback();

    assertThat(segredosUsados(jobDeRollback))
        .containsExactlyInAnyOrder("RENDER_API_KEY", "RENDER_SERVICE_ID", "SERVICE_URL");
    assertThat(jobDeRollback).doesNotContainKey("environment");
  }

  @Test
  @DisplayName("O rollback não move a tag main no GHCR nem envia imgURL (ADR 0013)")
  void rollbackNaoUsaImagemDoGhcr() throws IOException {
    var workflow = Files.readString(ROLLBACK);

    assertThat(workflow)
        .doesNotContain("imgURL")
        .doesNotContain("imagetools")
        .doesNotContain("ghcr.io");
  }

  @Test
  @DisplayName("Nenhum workflow define o intervalo, o prazo ou o endereço da API do Render (O13)")
  void nenhumWorkflowDefineIntervaloPrazoOuEnderecoDaApi() throws IOException {
    for (var workflow : new Path[] {CI, ROLLBACK}) {
      assertThat(Files.readString(workflow))
          .as(workflow.toString())
          .doesNotContain("ESPERA_INTERVALO_SEGUNDOS")
          .doesNotContain("ESPERA_PRAZO_SEGUNDOS")
          .doesNotContain("api.render.com")
          .doesNotContain("RENDER_API_URL")
          .doesNotContain("LIMITE_DA_CHAMADA_SEGUNDOS");
    }
  }

  @Test
  @DisplayName(
      "O script de rollback fixa o endereço da API do Render, sem leitura de variável (O5)")
  void scriptDeRollbackFixaOEnderecoDaApi() throws IOException {
    var script = Files.readString(SCRIPT_DE_ROLLBACK);

    assertThat(script)
        .contains("readonly URL_DA_API_DO_RENDER=\"https://api.render.com\"")
        .doesNotContain(":-");
  }

  @Test
  @DisplayName("Os jobs de deploy e de rollback têm timeout-minutes: 15 (CA59)")
  void jobsDeDeployERollbackTemTempoLimiteDe15Minutos() throws IOException {
    assertThat(mapa(jobs(CI).get("deploy"))).containsEntry("timeout-minutes", 15);
    assertThat(jobDeRollback()).containsEntry("timeout-minutes", 15);
  }

  private static Map<String, Object> jobDeRollback() throws IOException {
    return mapa(jobs(ROLLBACK).get("rollback"));
  }

  private static Map<String, Object> jobs(Path workflow) throws IOException {
    Map<String, Object> conteudo = new Yaml().load(Files.readString(workflow));
    return mapa(conteudo.get("jobs"));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> mapa(Object valor) {
    return (Map<String, Object>) valor;
  }

  private static Set<String> segredosUsados(Map<String, Object> job) {
    var segredos = new TreeSet<String>();
    var procura = SEGREDO.matcher(new Yaml().dump(job));
    while (procura.find()) {
      segredos.add(procura.group(1));
    }
    return segredos;
  }
}
