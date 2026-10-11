package cifra.ledger.internal;

import cifra.ledger.Lado;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * El hash de un asiento se calcula con su contenido Y con el hash del anterior. Cambiar cualquier
 * dato de un asiento antiguo cambia su hash, y con el, el de todos los que vienen despues.
 */
final class HashDeAsiento {

    static final String GENESIS = "GENESIS";

    private HashDeAsiento() {}

    /** La forma canonica de una linea: siempre con cuatro decimales, para que escribir y verificar coincidan. */
    static String linea(String codigoCuenta, Lado lado, BigDecimal importe) {
        return codigoCuenta + ":" + lado + ":" + importe.setScale(4).toPlainString();
    }

    static String calcular(String hashAnterior, UUID tenant, long secuencia, LocalDate fecha,
                           String descripcion, UUID reversaDe, List<String> lineas) {
        var texto = new StringBuilder()
                .append(hashAnterior).append('|')
                .append(tenant).append('|')
                .append(secuencia).append('|')
                .append(fecha).append('|')
                .append(descripcion).append('|')
                .append(reversaDe == null ? "-" : reversaDe.toString());
        lineas.stream().sorted().forEach(l -> texto.append('|').append(l));
        try {
            var resumen = MessageDigest.getInstance("SHA-256").digest(texto.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible en esta JVM", e);
        }
    }
}
