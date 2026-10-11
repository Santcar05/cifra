package cifra.ledger;

/**
 * Los cinco tipos de cuenta y su naturaleza: cual de los dos lados aumenta su
 * saldo
 */
public enum TipoCuenta {
    ACTIVO(true),
    PASIVO(false),
    PATRIMONIO(false),
    INGRESO(false),
    GASTO(true);

    private final boolean naturalezaDeudora;

    TipoCuenta(boolean naturalezaDeudora) {
        this.naturalezaDeudora = naturalezaDeudora;
    }

    /* Verdadero si el debito aumenta el saldo (activos y gastos); falso si lo aumenta al credito */
    public boolean esDeudora() {
        return naturalezaDeudora;
    }
}
