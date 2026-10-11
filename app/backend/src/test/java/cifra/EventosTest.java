package cifra;

import static cifra.ledger.NuevaLinea.credito;
import static cifra.ledger.NuevaLinea.debito;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import cifra.apartado.OyenteInestable;
import cifra.ledger.LedgerApi;
import cifra.ledger.NuevoAsiento;
import cifra.shared.Money;
import cifra.shared.persistencia.TenantSession;
import cifra.soporte.PostgresTestConfig;
import cifra.soporte.Tenants;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
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

    @Autowired LedgerApi ledger;
    @Autowired TenantSession sesion;
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

    private void asentarAlgo(String descripcion) {
        sesion.como(Tenants.PROPIETARIO, () -> ledger.registrar(new NuevoAsiento(LocalDate.of(2026, 9, 10), descripcion,
                List.of(debito("1110", Money.cop("100")), credito("4135", Money.cop("100"))))));
    }

    @Test
    void el_evento_se_entrega_y_queda_registrado_como_completado() {
        asentarAlgo("prueba");

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(contar("completion_date IS NOT NULL")).isEqualTo(2));
        assertThat(contar("completion_date IS NULL")).isZero();
    }

    @Test
    void si_un_oyente_falla_el_evento_no_se_pierde_y_se_puede_reenviar() {
        OyenteInestable.falla = true;
        asentarAlgo("con fallo");

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(contar("completion_date IS NULL")).isEqualTo(1));
        assertThat(contar("completion_date IS NOT NULL")).isEqualTo(1);

        OyenteInestable.falla = false;
        incompletas.resubmitIncompletePublications(publicacion -> true);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(contar("completion_date IS NULL")).isZero());
    }
}
