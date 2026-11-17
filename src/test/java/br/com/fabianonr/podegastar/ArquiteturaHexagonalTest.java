package br.com.fabianonr.podegastar;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArquiteturaHexagonalTest {

  private static final String PACOTE_RAIZ = "br.com.fabianonr.podegastar";

  private static JavaClasses classesDeProducao;

  @BeforeAll
  static void importarClassesDeProducao() {
    classesDeProducao =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(PACOTE_RAIZ);
  }

  @Test
  @DisplayName("Domínio não depende de web, persistência nem infraestrutura (ADR 0003)")
  void dominioNaoDependeDeWebPersistenciaNemInfraestrutura() {
    noClasses()
        .that()
        .resideInAPackage("..dominio..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..adaptador..",
            "..configuracao..",
            "org.springframework..",
            "com.fasterxml.jackson..",
            "jakarta.persistence..",
            "jakarta.servlet..",
            "java.sql..",
            "javax.sql..")
        .because("o núcleo de domínio não conhece Spring nem JSON (ADR 0003)")
        .check(classesDeProducao);
  }

  @Test
  @DisplayName("Portas de entrada e de saída ficam no domínio ou na aplicação (ADR 0003)")
  void portasFicamNoDominioOuNaAplicacao() {
    classes()
        .that()
        .resideInAnyPackage("..porta.entrada..", "..porta.saida..")
        .should()
        .resideInAnyPackage("..dominio..", "..aplicacao..")
        .because("adaptadores dependem das portas, e não o contrário (ADR 0003)")
        .check(classesDeProducao);
  }

  @Test
  @DisplayName("Um adaptador não acessa outro adaptador diretamente (ADR 0003)")
  void adaptadoresNaoAcessamOutrosAdaptadores() {
    slices()
        .matching(PACOTE_RAIZ + ".adaptador.(*).(*)..")
        .should()
        .notDependOnEachOther()
        .because("adaptadores conversam só com as portas (ADR 0003)")
        .check(classesDeProducao);
  }
}
