package cifra.ledger;

import cifra.shared.Money;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * La unica puerta del ledger. Todo lo que otro modulo puede pedirle esta aqui.
 * Nada de lo que hay detras (entidades, repositorios, el calculo del hash) es
 * visible desde fuera.
 */
public interface LedgerApi {

    /**
     * El ledger se lleva en pesos colombianos. El origen extranjero de una
     * linea se guarda aparte.
     */
    Currency MONEDA_FUNCIONAL = Currency.getInstance("COP");

    /**
     * Registra un asiento en el tenant del contexto actual. Corre en la
     * transaccion del que llama.
     */
    Asiento registrar(NuevoAsiento asiento);

    /**
     * Corrige un asiento sin tocarlo: registra otro con los lados invertidos,
     * enlazado al original. Despues se registra el asiento correcto. No se
     * puede revertir una reversion ni revertir dos veces.
     */
    Asiento revertir(UUID asientoId, LocalDate fecha, String motivo);

    /**
     * Saldo de una cuenta a una fecha, con el signo de su naturaleza (un activo
     * con saldo normal es positivo).
     */
    Money saldo(String codigoCuenta, LocalDate hasta);

    /**
     * El saldo de todas las cuentas a una fecha.
     */
    List<SaldoDeCuenta> balanceDeComprobacion(LocalDate hasta);

    /**
     * Recorre la cadena de hashes del tenant y dice si algo fue alterado.
     */
    VerificacionDeCadena verificarCadena();
}
