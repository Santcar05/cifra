package cifra.ledger.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * La cabecera de la cadena de un tenant. A diferencia de los asientos, ESTA
 * fila se actualiza: guarda el ultimo hash y la ultima secuencia, y al
 * bloquearla se serializa el registro de asientos.
 */
@Entity
@Table(name = "cadena_asientos")
class CadenaEntity {

    @Id
    private UUID tenantId;
    private long ultimaSecuencia;
    private String ultimoHash;

    protected CadenaEntity() {
    }

    long ultimaSecuencia() {
        return ultimaSecuencia;
    }

    String ultimoHash() {
        return ultimoHash;
    }

    void avanzar(long secuencia, String hash) {
        this.ultimaSecuencia = secuencia;
        this.ultimoHash = hash;
    }
}
