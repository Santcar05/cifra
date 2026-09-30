package cifra.shared.persistencia;

import cifra.shared.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TenantJpaTransactionManager extends JpaTransactionManager {

    static final String FIJAR_TENANT = "select set_config('app.tenant_id', :tenant, true)";

    public TenantJpaTransactionManager(EntityManagerFactory emf) {
        super(emf);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        TenantContext.actual().ifPresent(tenant -> {
            var holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
            fijarTenant(holder.getEntityManager(), tenant.toString());
        });
    }

    static void fijarTenant(EntityManager em, String tenant) {
        em.createNativeQuery(FIJAR_TENANT).setParameter("tenant", tenant).getSingleResult();
    }
}
