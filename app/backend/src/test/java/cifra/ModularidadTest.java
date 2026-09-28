package cifra;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularidadTest {

    static final ApplicationModules modulos = ApplicationModules.of(CifraApplication.class);

    @Test
    void las_fronteras_entre_modulos_se_respetan() {
        modulos.verify();
    }

    @Test
    void imprime_los_modulos_detectados() {
        modulos.forEach(System.out::println);
    }

    @Test
    void genera_la_documentacion_de_arquitectura() {
        new Documenter(modulos).writeDocumentation();
    }
}
