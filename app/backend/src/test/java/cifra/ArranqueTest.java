package test.java.cifra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

import cifra.soporte.PostgresTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class ArranqueTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void la_aplicacion_arranca_contra_postgres_real() {
        var respuesta = mvc.get().uri("/api/v1/sistema/estado")
                .with(httpBasic("santiago", "cifra-dev-local"));
        assertThat(respuesta).hasStatusOk()
                .bodyJson().extractingPath("$.aplicacion").isEqualTo("cifra-backend");
    }

    @Test
    void una_version_inexistente_se_rechaza() {
        assertThat(mvc.get().uri("/api/v9/sistema/estado")
                .with(httpBasic("santiago", "cifra-dev-local")))
                .hasStatus4xxClientError();
    }

    @Test
    void sin_credenciales_la_api_responde_401() {
        assertThat(mvc.get().uri("/api/v1/sistema/estado")).hasStatus(401);
    }
}
