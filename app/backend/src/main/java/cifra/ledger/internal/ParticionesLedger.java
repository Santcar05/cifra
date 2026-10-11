package cifra.ledger.internal;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mantiene creadas las particiones mensuales del ledger (ver asegurar_particion en la migracion V5). */
@Component
class ParticionesLedger {

    private static final Logger log = LoggerFactory.getLogger(ParticionesLedger.class);
    private static final int MESES_POR_ADELANTADO = 6;

    private final JdbcTemplate jdbc;
    private final Set<YearMonth> aseguradas = ConcurrentHashMap.newKeySet();

    ParticionesLedger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Se llama antes de insertar: si el mes ya se aseguro en esta JVM, no hace nada. */
    void asegurar(LocalDate fecha) {
        var mes = YearMonth.from(fecha);
        if (aseguradas.contains(mes)) {
            return;
        }
        jdbc.queryForList("SELECT asegurar_particion(?)", fecha);
        aseguradas.add(mes);
    }

    /** El primer dia de cada mes, a las 3:00, crea las particiones de los proximos meses. */
    @Scheduled(cron = "0 0 3 1 * *")
    void asegurarProximosMeses() {
        var hoy = LocalDate.now();
        for (int i = 0; i <= MESES_POR_ADELANTADO; i++) {
            asegurar(hoy.plusMonths(i));
        }
        log.info("Particiones del ledger aseguradas hasta {}", hoy.plusMonths(MESES_POR_ADELANTADO));
    }
}
