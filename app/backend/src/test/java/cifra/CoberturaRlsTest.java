package cifra;

import static org.assertj.core.api.Assertions.assertThat;

import cifra.soporte.PostgresTestConfig;
import cifra.soporte.PostgresTestConfig.AdminDb;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Toda tabla con una columna tenant_id debe tener RLS activado y forzado. Esta prueba existe
 * porque la V4 lo olvido en tres tablas y nada fallo (ver operacion/postmortems/0001).
 * Una tabla nueva sin RLS rompe el CI en vez de convertirse en una fuga entre tenants.
 */
@SpringBootTest
@Import(PostgresTestConfig.class)
class CoberturaRlsTest {

    @Autowired AdminDb admin;

    @Test
    void toda_tabla_con_tenant_id_tiene_rls_activado_y_forzado() {
        // Las particiones se excluyen: se consultan a traves de la tabla madre, que es la que
        // aplica la politica, y la aplicacion no tiene permisos sobre ellas directamente.
        var sinRls = admin.jdbc().queryForList("""
                SELECT c.relname
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attname = 'tenant_id' AND NOT a.attisdropped
                WHERE n.nspname = 'public'
                  AND c.relkind IN ('r', 'p')
                  AND NOT c.relispartition
                  AND NOT (c.relrowsecurity AND c.relforcerowsecurity)
                ORDER BY c.relname
                """, String.class);

        assertThat(sinRls).as("tablas con tenant_id sin ENABLE + FORCE ROW LEVEL SECURITY").isEmpty();
    }

    @Test
    void toda_tabla_con_rls_tiene_al_menos_una_politica() {
        // RLS sin politica no filtra: lo niega todo. No es una fuga, pero si un error silencioso.
        var sinPolitica = admin.jdbc().queryForList("""
                SELECT c.relname
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND c.relrowsecurity
                  AND NOT c.relispartition
                  AND NOT EXISTS (SELECT 1 FROM pg_policy p WHERE p.polrelid = c.oid)
                ORDER BY c.relname
                """, String.class);

        assertThat(sinPolitica).as("tablas con RLS pero sin politica").isEmpty();
    }
}
