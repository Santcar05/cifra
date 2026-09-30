package cifra.shared.persistencia;

import cifra.shared.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class TenantSession {

    @PersistenceContext
    private EntityManager em;

    public <T> T como(UUID tenant, Supplier<T> operacion) {
        return TenantContext.como(tenant, () -> {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TenantJpaTransactionManager.fijarTenant(em, tenant.toString());
            }
            return operacion.get();
        });
    }

    public void como(UUID tenant, Runnable operacion) {
        como(tenant, () -> {
            operacion.run();
            return null;
        });
    }
}
