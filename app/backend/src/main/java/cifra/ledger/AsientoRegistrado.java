package cifra.ledger;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento que el ledger publica cuando registra un asiento. Lleva el tenant:
 * los oyentes corren en otro hilo.
 */
public record AsientoRegistrado(UUID tenantId, UUID asientoId, long secuencia, LocalDate fecha, String descripcion) {

}
