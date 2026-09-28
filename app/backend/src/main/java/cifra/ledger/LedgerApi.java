package cifra.ledger;

import java.time.LocalDate;
import java.util.UUID;

public interface LedgerApi {

    UUID registrar(LocalDate fecha, String descripcion);
}
