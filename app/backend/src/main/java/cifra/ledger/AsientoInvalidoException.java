package cifra.ledger;

/**
 * Un asiento que incumple una regla contable: descuadrado, sin lineas
 * suficientes, en otra moneda...
 **/
public class AsientoInvalidoException extends RuntimeException {

    public AsientoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
