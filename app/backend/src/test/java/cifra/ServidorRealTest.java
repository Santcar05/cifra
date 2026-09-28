package cifra;

import static org.assertj.core.api.Assertions.assertThat;

import cifra.soporte.PostgresTestConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
@Import(PostgresTestConfig.class)
class ServidorRealTest {

    @LocalServerPort int puerto;
    @LocalManagementPort int puertoGestion;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(int p, String ruta) throws Exception {
        var credenciales = Base64.getEncoder().encodeToString("santiago:cifra-dev-local".getBytes());
        var peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + p + ruta))
                .header("Authorization", "Basic " + credenciales)
                .build();
        return http.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void las_peticiones_corren_en_hilos_virtuales() throws Exception {
        var r = get(puerto, "/api/v1/sistema/estado");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("VirtualThread");
    }

    @Test
    void actuator_solo_responde_en_el_puerto_de_gestion() throws Exception {
        var sinCredenciales = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puertoGestion + "/actuator/health")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(sinCredenciales.statusCode()).isEqualTo(200);
        assertThat(sinCredenciales.body()).contains("UP");
        assertThat(get(puerto, "/actuator/health").statusCode()).isEqualTo(404);
    }

    @Test
    void version_no_soportada_devuelve_400_con_problem_details() throws Exception {
        var r = get(puerto, "/api/v9/sistema/estado");
        assertThat(r.statusCode()).isEqualTo(400);
        assertThat(r.body()).contains("\"title\":\"Bad Request\"");
    }
}
