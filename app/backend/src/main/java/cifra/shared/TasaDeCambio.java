package cifra.shared;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TasaDeCambio(BigDecimal valor, LocalDate fecha) {

    public static TasaDeCambio of(String valor, LocalDate fecha) {
        return new TasaDeCambio(new BigDecimal(valor), fecha);
    }
}
