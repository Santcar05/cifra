package cifra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import cifra.apartado.OyenteInestable;
import cifra.facturacion.FacturacionApi;
import cifra.soporte.PostgresTestConfig;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;

@SpringBootTest
@Import({PostgresTestConfig.class, OyenteInestable.class})
class EventosTest {

    @Autowired FacturacionApi facturacion;
    @Autowired JdbcTemplate jdbc;
    @Autowired IncompleteEventPublications incompletas;

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM event_publication");
        OyenteInestable.falla = false;
    }

    private int contar(String condicion) {
        return jdbc.queryForObject("SELECT count(*) FROM event_publication WHERE " + condicion, Integer.class);
    }

    @Test
    void el_evento_se_entrega_y_queda_registrado_como_completado() {
        facturacion.emitir("prueba");

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(contar("completion_date IS NOT NULL")).isEqualTo(2));
        assertThat(contar("completion_date IS NULL")).isZero();
    }

    @Test
    void si_un_oyente_falla_el_evento_no_se_pierde_y_se_puede_reenviar() {
        OyenteInestable.falla = true;
        facturacion.emitir("con fallo");

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(contar("completion_date IS NULL")).isEqualTo(1));
        assertThat(contar("completion_date IS NOT NULL")).isEqualTo(1);

        OyenteInestable.falla = false;
        incompletas.resubmitIncompletePublications(publicacion -> true);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(contar("completion_date IS NULL")).isZero());
    }
}
