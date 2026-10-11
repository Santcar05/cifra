package cifra.ledger.internal;

import cifra.ledger.Lado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Immutable
@Table(name = "linea_asiento")
@IdClass(LineaAsientoEntity.Clave.class)
class LineaAsientoEntity {

    record Clave(UUID id, LocalDate fecha) implements Serializable {

    }

    @Id
    private UUID id;
    @Id
    private LocalDate fecha;

    private UUID asientoId;
    private UUID tenantId;
    private UUID cuentaId;

    @Enumerated(EnumType.STRING)
    private Lado lado;

    @Column(precision = 19, scale = 4)
    private BigDecimal importe;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private String monedaOrigen;

    @Column(precision = 19, scale = 4)
    private BigDecimal importeOrigen;

    @Column(precision = 19, scale = 6)
    private BigDecimal trm;

    protected LineaAsientoEntity() {
    }

    LineaAsientoEntity(UUID id, LocalDate fecha, UUID asientoId, UUID tenantId, UUID cuentaId, Lado lado,
            BigDecimal importe, String monedaOrigen, BigDecimal importeOrigen, BigDecimal trm) {
        this.id = id;
        this.fecha = fecha;
        this.asientoId = asientoId;
        this.tenantId = tenantId;
        this.cuentaId = cuentaId;
        this.lado = lado;
        this.importe = importe;
        this.monedaOrigen = monedaOrigen;
        this.importeOrigen = importeOrigen;
        this.trm = trm;
    }

    UUID asientoId() {
        return asientoId;
    }

    UUID cuentaId() {
        return cuentaId;
    }

    Lado lado() {
        return lado;
    }

    BigDecimal importe() {
        return importe;
    }
}
