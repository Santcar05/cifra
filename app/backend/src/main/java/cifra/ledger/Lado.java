package cifra.ledger;

public enum Lado {
    DEBITO,
    CREDITO;

    public Lado opuesto() {
        return this == DEBITO ? CREDITO : DEBITO;
    }
}
