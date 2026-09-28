package cifra.ledger.internal;

import cifra.ledger.AsientoRegistrado;
import cifra.ledger.LedgerApi;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LedgerServiceImpl implements LedgerApi {

    private final ApplicationEventPublisher eventos;

    LedgerServiceImpl(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    @Override
    @Transactional
    public UUID registrar(LocalDate fecha, String descripcion) {
        var id = UUID.randomUUID();
        eventos.publishEvent(new AsientoRegistrado(id, fecha, descripcion));
        return id;
    }
}
