package cifra.ledger.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Un asiento guardado. @Immutable: Hibernate nunca emite un UPDATE sobre esta entidad. */
@Entity
@Immutable
@Table(name = "asiento")
@IdClass(AsientoEntity.Clave.class)
class AsientoEntity {

    /** La clave incluye la fecha porque la tabla esta particionada por ella. */
    record Clave(UUID id, LocalDate fecha) implements Serializable {}

    @Id
    private UUID id;
    @Id
    private LocalDate fecha;

    private UUID tenantId;
    private long secuencia;
    private String descripcion;
    private UUID reversaDe;
    private String hashAnterior;
    private String hash;

    protected AsientoEntity() {}

    AsientoEntity(UUID id, LocalDate fecha, UUID tenantId, long secuencia, String descripcion,
                  UUID reversaDe, String hashAnterior, String hash) {
        this.id = id;
        this.fecha = fecha;
        this.tenantId = tenantId;
        this.secuencia = secuencia;
        this.descripcion = descripcion;
        this.reversaDe = reversaDe;
        this.hashAnterior = hashAnterior;
        this.hash = hash;
    }

    UUID id() { return id; }
    LocalDate fecha() { return fecha; }
    long secuencia() { return secuencia; }
    String descripcion() { return descripcion; }
    UUID reversaDe() { return reversaDe; }
    String hashAnterior() { return hashAnterior; }
    String hash() { return hash; }
}
