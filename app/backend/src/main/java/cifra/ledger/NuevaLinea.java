package cifra.ledger;

import cifra.shared.Money;
import java.util.Objects;

/**
 * Una linea de un asiento por registrar: una cuenta (por su codigo), un lado y
 * un importe positivo en pesos.
 */
public record NuevaLinea(String cuenta, Lado lado, Money importe, OrigenExtranjero origen) {

    public NuevaLinea {
        Objects.requireNonNull(cuenta, "La cuenta es obligatoria");
        Objects.requireNonNull(lado, "El lado es obligatorio");
        Objects.requireNonNull(importe, "El importe es obligatorio");
        if (importe.amount().signum() <= 0) {
            throw new AsientoInvalidoException("El importe de una linea debe ser positivo; el lado dice si suma o resta");
        }
    }

    public static NuevaLinea debito(String cuenta, Money importe) {
        return new NuevaLinea(cuenta, Lado.DEBITO, importe, null);
    }

    public static NuevaLinea credito(String cuenta, Money importe) {
        return new NuevaLinea(cuenta, Lado.CREDITO, importe, null);
    }

    public NuevaLinea conOrigen(OrigenExtranjero origen) {
        return new NuevaLinea(cuenta, lado, importe, origen);
    }
}
