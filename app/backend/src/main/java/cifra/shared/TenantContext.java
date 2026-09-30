package cifra.shared;

import java.util.Optional;
import java.util.UUID;

/**
 * El tenant "actual" del hilo que ejecuta el codigo. Se fija una vez (por el
 * filtro de seguridad al llegar una peticion) y vale para todo lo que se
 * ejecute dentro.
 */
public final class TenantContext {

    private static final ScopedValue<UUID> TENANT = ScopedValue.newInstance();

    private TenantContext() {
    }

    public static Optional<UUID> actual() {
        return TENANT.isBound() ? Optional.of(TENANT.get()) : Optional.empty();
    }

    public static UUID requerido() {
        return actual().orElseThrow(() -> new IllegalStateException("No hay un tenant en el contexto actual"));
    }

    public static <T, X extends Throwable> T como(UUID tenant, ScopedValue.CallableOp<? extends T, X> operacion) throws X {
        return ScopedValue.where(TENANT, tenant).call(operacion);
    }

    public static void como(UUID tenant, Runnable operacion) {
        ScopedValue.where(TENANT, tenant).run(operacion);
    }
}
