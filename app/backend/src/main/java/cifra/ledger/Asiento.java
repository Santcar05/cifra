package cifra.ledger;

import cifra.shared.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Un asiento ya registrado. Es una vista de solo lectura: el ledger no admite
 * cambios.
 */
public record Asiento(
        UUID id,
        long secuencia,
        LocalDate fecha,
        String descripcion,
        UUID reversaDe,
        String hash,
        List<LineaContable> lineas) {

    public record LineaContable(String cuenta, String nombreCuenta, Lado lado, Money importe) {

    }
}
