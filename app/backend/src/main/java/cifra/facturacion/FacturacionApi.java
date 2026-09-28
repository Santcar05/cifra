package cifra.facturacion;

import cifra.ledger.LedgerApi;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacturacionApi {

    private final LedgerApi ledger;

    FacturacionApi(LedgerApi ledger) {
        this.ledger = ledger;
    }

    @Transactional
    public UUID emitir(String descripcion) {
        return ledger.registrar(LocalDate.now(), "Factura: " + descripcion);
    }
}
