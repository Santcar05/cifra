package cifra.ledger;

import java.time.LocalDate;
import java.util.UUID;

public record AsientoRegistrado(UUID asientoId, LocalDate fecha, String descripcion) {

}
