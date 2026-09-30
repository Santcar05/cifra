package cifra.soporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfig {

    public static final String CLAVE_APP = "cifra_app_test";

    /**
     * Conexion de superusuario, solo para preparar y comprobar datos desde las
     * pruebas. Va envuelta en un tipo propio: si fuera un bean JdbcTemplate,
     * Spring Boot no crearia el JdbcTemplate normal (el de la aplicacion) y las
     * pruebas usarian sin querer el de admin.
     */
    public record AdminDb(JdbcTemplate jdbc) {

    }

    @Bean
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18");
    }

    @Bean
    DynamicPropertyRegistrar propiedadesDeConexion(PostgreSQLContainer postgres) {
        return registro -> {
            registro.add("spring.flyway.url", postgres::getJdbcUrl);
            registro.add("spring.flyway.user", postgres::getUsername);
            registro.add("spring.flyway.password", postgres::getPassword);
            registro.add("spring.flyway.placeholders.app_password", () -> CLAVE_APP);
            registro.add("spring.datasource.url", postgres::getJdbcUrl);
            registro.add("spring.datasource.username", () -> "cifra_app");
            registro.add("spring.datasource.password", () -> CLAVE_APP);
        };
    }

    @Bean
    AdminDb admin(PostgreSQLContainer postgres) {
        var origen = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        return new AdminDb(new JdbcTemplate(origen));
    }
}
