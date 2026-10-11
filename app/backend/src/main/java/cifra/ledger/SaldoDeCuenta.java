package cifra.ledger;

import cifra.shared.Money;

public record SaldoDeCuenta(String codigo, String nombre, TipoCuenta tipo, Money saldo) {

}
