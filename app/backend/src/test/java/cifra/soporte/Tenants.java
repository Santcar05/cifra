package cifra.soporte;

import cifra.soporte.PostgresTestConfig.AdminDb;
import java.util.UUID;

/** Da de alta un tenant de prueba con su plan de cuentas, como lo haria el onboarding. */
public final class Tenants {

    public static final UUID PROPIETARIO = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Tenants() {}

    public static UUID nuevo(AdminDb admin) {
        var id = UUID.randomUUID();
        admin.jdbc().update("INSERT INTO tenant(id, nombre) VALUES (?, ?)", id, "tenant-" + id);
        admin.jdbc().queryForList("SELECT sembrar_plan_de_cuentas(?)", id);
        return id;
    }
}
