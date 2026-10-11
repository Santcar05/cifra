package cifra.ledger.internal;

import java.util.UUID;

import org.hibernate.annotations.Immutable;

import cifra.ledger.TipoCuenta;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "cuenta")
class CuentaEntity {

    @Id
    private UUID id;
    private UUID tenantId;
    private String codigo;
    private String nombre;

    @Enumerated(EnumType.STRING)
    private TipoCuenta tipo;

    protected CuentaEntity() {
    }

    UUID id() {
        return id;
    }

    String codigo() {
        return codigo;
    }

    String nombre() {
        return nombre;
    }

    TipoCuenta tipo() {
        return tipo;
    }
}
