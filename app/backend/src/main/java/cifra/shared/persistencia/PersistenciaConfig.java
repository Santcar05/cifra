package cifra.shared.persistencia;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
class PersistenciaConfig {

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new TenantJpaTransactionManager(emf);
    }
}
