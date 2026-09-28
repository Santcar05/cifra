package cifra.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

    private static final RoundingMode REDONDEO_FISCAL = RoundingMode.HALF_UP;

    // El constructor compacto normaliza y valida en cada construcción
    public Money {
        Objects.requireNonNull(amount, "El importe no puede ser nulo");
        Objects.requireNonNull(currency, "La moneda no puede ser nula");
        amount = amount.setScale(currency.getDefaultFractionDigits(), REDONDEO_FISCAL);
    }

    // Única puerta de entrada desde texto — nunca desde double
    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money cop(String amount) {
        return of(amount, "COP");
    }

    public Money add(Money otro) {
        requireMismaMoneda(otro);
        return new Money(this.amount.add(otro.amount), this.currency);
    }

    public Money subtract(Money otro) {
        requireMismaMoneda(otro);
        return new Money(this.amount.subtract(otro.amount), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public boolean isGreaterThan(Money otro) {
        requireMismaMoneda(otro);
        return this.amount.compareTo(otro.amount) > 0;
    }

    public boolean equalsValue(Money otro) {
        requireMismaMoneda(otro);
        return this.amount.compareTo(otro.amount) == 0;
    }

    private void requireMismaMoneda(Money otro) {
        if (!this.currency.equals(otro.currency)) {
            throw new IllegalArgumentException(
                "No se pueden operar monedas distintas: " + this.currency + " y " + otro.currency);
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
