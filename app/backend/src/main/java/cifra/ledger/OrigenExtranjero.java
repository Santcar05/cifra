package cifra.ledger;

import java.math.BigDecimal;
import cifra.shared.Money;
import java.util.Objects;

public record OrigenExtranjero(Money importeOrigen, BigDecimal trm) {

    public OrigenExtranjero {
        Objects.requireNonNull(importeOrigen, "El importe de origen es obligatorio");
        Objects.requireNonNull(trm, "La TRM es obligatoria");

        if (trm.signum() <= 0) {
            throw new AsientoInvalidoException("La TRM debe ser positiva");
        }
    }
}
