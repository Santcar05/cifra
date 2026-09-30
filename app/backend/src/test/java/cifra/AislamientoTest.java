package cifra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cifra.shared.persistencia.TenantSession;
import cifra.soporte.PostgresTestConfig;
import cifra.soporte.PostgresTestConfig.AdminDb;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=1")
@Import(PostgresTestConfig.class)
class AislamientoTest {

    static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    static final LocalDate FECHA = LocalDate.of(2026, 9, 10);

    @Autowired JdbcTemplate app;
    @Autowired AdminDb admin;
    @Autowired TransactionTemplate tx;
    @Autowired TenantSession sesion;

    @BeforeEach
    void preparar() {
        for (var tenant : new UUID[] {A, B}) {
            admin.jdbc().update("INSERT INTO tenant(id, nombre) VALUES (?, ?) ON CONFLICT DO NOTHING", tenant, "T-" + tenant);
        }
    }

    private <T> T como(UUID tenant, Supplier<T> operacion) {
        return sesion.como(tenant, () -> tx.execute(estado -> operacion.get()));
    }

    private UUID cuenta(UUID tenant, String codigo, String tipo) {
        var id = UUID.randomUUID();
        admin.jdbc().update("INSERT INTO cuenta(id, tenant_id, codigo, nombre, tipo) VALUES (?,?,?,?,?)",
                id, tenant, codigo + "-" + id, "cuenta " + codigo, tipo);
        return id;
    }

    private void insertarAsiento(UUID tenant, UUID debe, UUID haber, String debitos, String creditos) {
        var asiento = UUID.randomUUID();
        app.update("INSERT INTO asiento(id, tenant_id, secuencia, fecha, descripcion, hash_anterior, hash) VALUES (?,?,?,?,?,?,?)",
                asiento, tenant, 1, FECHA, "prueba", "GENESIS", "h");
        app.update("INSERT INTO linea_asiento(id, asiento_id, tenant_id, fecha, cuenta_id, lado, importe) VALUES (?,?,?,?,?,'DEBITO',?::numeric)",
                UUID.randomUUID(), asiento, tenant, FECHA, debe, debitos);
        app.update("INSERT INTO linea_asiento(id, asiento_id, tenant_id, fecha, cuenta_id, lado, importe) VALUES (?,?,?,?,?,'CREDITO',?::numeric)",
                UUID.randomUUID(), asiento, tenant, FECHA, haber, creditos);
    }

    @Test
    void la_aplicacion_no_se_conecta_como_superusuario_ni_con_bypass_de_rls() {
        var fila = app.queryForMap("SELECT rolsuper, rolbypassrls FROM pg_roles WHERE rolname = current_user");
        assertThat(fila).containsEntry("rolsuper", false).containsEntry("rolbypassrls", false);
    }

    @Test
    void sin_tenant_en_el_contexto_no_se_ve_nada() {
        var visibles = tx.execute(estado -> app.queryForObject("SELECT count(*) FROM tenant", Integer.class));
        assertThat(visibles).isZero();
    }

    @Test
    void cada_tenant_ve_solo_lo_suyo() {
        assertThat(como(A, () -> app.queryForList("SELECT id FROM tenant", UUID.class))).containsExactly(A);
        assertThat(como(B, () -> app.queryForList("SELECT id FROM tenant", UUID.class))).containsExactly(B);
    }

    @Test
    void olvidar_el_where_no_devuelve_datos_de_otro_tenant() {
        cuenta(A, "1110", "ACTIVO");
        cuenta(A, "4135", "INGRESO");
        cuenta(B, "1110", "ACTIVO");

        var deA = como(A, () -> app.queryForObject("SELECT count(*) FROM cuenta", Integer.class));
        var deB = como(B, () -> app.queryForObject("SELECT count(*) FROM cuenta", Integer.class));

        assertThat(deA).isEqualTo(2);
        assertThat(deB).isEqualTo(1);
    }

    @Test
    void insertar_a_nombre_de_otro_tenant_falla() {
        assertThatThrownBy(() -> como(A, () -> app.update(
                "INSERT INTO cuenta(id, tenant_id, codigo, nombre, tipo) VALUES (?,?,?,?,?)",
                UUID.randomUUID(), B, "9999", "intrusa", "ACTIVO")))
                .isInstanceOf(DataAccessException.class)
                .rootCause().hasMessageContaining("row-level security");
    }

    @Test
    void el_tenant_de_una_transaccion_no_se_filtra_a_la_siguiente_por_el_pool() {
        como(A, () -> app.queryForObject("SELECT count(*) FROM tenant", Integer.class));

        var siguiente = tx.execute(estado -> app.queryForObject("SELECT count(*) FROM tenant", Integer.class));

        assertThat(siguiente).as("con un pool de una sola conexion, la misma se reutiliza").isZero();
    }

    @Test
    void un_asiento_cuadrado_se_guarda() {
        var debe = cuenta(A, "1110", "ACTIVO");
        var haber = cuenta(A, "4135", "INGRESO");

        como(A, () -> {
            insertarAsiento(A, debe, haber, "4000000", "4000000");
            return null;
        });

        assertThat(como(A, () -> app.queryForObject("SELECT count(*) FROM linea_asiento", Integer.class)))
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void un_asiento_descuadrado_no_llega_al_commit() {
        var debe = cuenta(A, "1110", "ACTIVO");
        var haber = cuenta(A, "4135", "INGRESO");

        assertThatThrownBy(() -> como(A, () -> {
            insertarAsiento(A, debe, haber, "4000000", "3999999");
            return null;
        })).hasMessageContaining("descuadrado");
    }

    @Test
    void la_aplicacion_no_puede_actualizar_ni_borrar_el_ledger() {
        assertThatThrownBy(() -> como(A, () -> app.update("UPDATE linea_asiento SET importe = 1")))
                .rootCause().hasMessageContaining("permission denied");
        assertThatThrownBy(() -> como(A, () -> app.update("DELETE FROM asiento")))
                .rootCause().hasMessageContaining("permission denied");
    }

    @Test
    void ni_siquiera_el_propietario_puede_modificar_un_asiento() {
        var debe = cuenta(A, "1110", "ACTIVO");
        var haber = cuenta(A, "4135", "INGRESO");
        como(A, () -> {
            insertarAsiento(A, debe, haber, "100", "100");
            return null;
        });

        assertThatThrownBy(() -> admin.jdbc().update("UPDATE linea_asiento SET importe = 1"))
                .hasMessageContaining("El ledger es inmutable");
        assertThatThrownBy(() -> admin.jdbc().update("DELETE FROM asiento"))
                .hasMessageContaining("El ledger es inmutable");
    }

    @Test
    void la_aplicacion_no_accede_directamente_a_las_particiones() {
        assertThatThrownBy(() -> como(A, () -> app.queryForObject("SELECT count(*) FROM asiento_2026_09", Integer.class)))
                .rootCause().hasMessageContaining("permission denied");
    }

    @Test
    void una_consulta_con_rango_de_fechas_toca_una_sola_particion() {
        var plan = como(A, () -> String.join("\n", app.queryForList(
                "EXPLAIN SELECT * FROM linea_asiento WHERE fecha >= '2026-09-01' AND fecha < '2026-10-01'",
                String.class)));

        assertThat(plan).contains("linea_asiento_2026_09").doesNotContain("linea_asiento_2026_08")
                .doesNotContain("linea_asiento_2026_10");
    }

    @Test
    void una_consulta_sin_rango_de_fechas_recorre_todas_las_particiones() {
        var plan = como(A, () -> String.join("\n", app.queryForList(
                "EXPLAIN SELECT * FROM linea_asiento WHERE cuenta_id = '" + UUID.randomUUID() + "'", String.class)));

        assertThat(plan).contains("linea_asiento_2025_01").contains("linea_asiento_2028_12");
    }

    @Test
    void la_aplicacion_puede_crear_particiones_futuras_solo_con_la_funcion() {
        assertThatThrownBy(() -> como(A, () -> app.update("CREATE TABLE intrusa(x int)")))
                .rootCause().hasMessageContaining("permission denied");

        como(A, () -> app.queryForList("SELECT asegurar_particion('2031-05-10')"));

        var existe = admin.jdbc().queryForObject("SELECT to_regclass('linea_asiento_2031_05') IS NOT NULL", Boolean.class);
        assertThat(existe).isTrue();
    }

    @Test
    void si_la_transaccion_ya_estaba_abierta_TenantSession_fija_el_tenant_en_ella() {
        var vistos = tx.execute(estado ->
                sesion.como(B, () -> app.queryForList("SELECT id FROM tenant", UUID.class)));

        assertThat(vistos).containsExactly(B);
    }
}
