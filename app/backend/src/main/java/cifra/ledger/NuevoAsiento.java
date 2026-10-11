package cifra.ledger;

import cifra.shared.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Un asiento por registrar. Si esta construido, ya cuadra: el constructor
 * rechaza los que no. La base de datos vuelve a comprobarlo al hacer commit;
 * esta es la primera linea de defensa, la que da un mensaje que se entiende.
 */
public record NuevoAsiento(LocalDate fecha, String descripcion, List<NuevaLinea> lineas) {

    public NuevoAsiento {
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        Objects.requireNonNull(descripcion, "La descripcion es obligatoria");
        Objects.requireNonNull(lineas, "Las lineas son obligatorias");
        if (descripcion.isBlank()) {
            throw new AsientoInvalidoException("La descripcion no puede estar vacia");
        }
        lineas = List.copyOf(lineas);
        if (lineas.size() < 2) {
            throw new AsientoInvalidoException("Un asiento necesita al menos dos lineas: todo hecho toca dos cuentas");
        }
        for (var linea : lineas) {
            if (!linea.importe().currency().equals(LedgerApi.MONEDA_FUNCIONAL)) {
                throw new AsientoInvalidoException("El ledger se lleva en " + LedgerApi.MONEDA_FUNCIONAL
                        + "; la linea de " + linea.cuenta() + " viene en " + linea.importe().currency());
            }
        }
        var debitos = total(lineas, Lado.DEBITO);
        var creditos = total(lineas, Lado.CREDITO);
        if (!debitos.equalsValue(creditos)) {
            throw new AsientoInvalidoException(
                    "Asiento descuadrado: debitos=" + debitos + " creditos=" + creditos);
        }
    }

    private static Money total(List<NuevaLinea> lineas, Lado lado) {
        return lineas.stream()
                .filter(l -> l.lado() == lado)
                .map(NuevaLinea::importe)
                .reduce(Money.cop("0"), Money::add);
    }

    public Money totalDebitos() {
        return total(lineas, Lado.DEBITO);
    }
}
