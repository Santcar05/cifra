package cifra.ledger;

/**
 * Resultado de recorrer la cadena de hashes. Si no es valida,
 * primeraSecuenciaInconsistente es el primer asiento cuyo contenido o cuyo
 * enlace con el anterior no coincide.
 */
public record VerificacionDeCadena(boolean valida, long asientosVerificados, Long primeraSecuenciaInconsistente) {

}
