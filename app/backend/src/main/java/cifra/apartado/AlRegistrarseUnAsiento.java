package cifra.apartado;

import cifra.ledger.AsientoRegistrado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class AlRegistrarseUnAsiento {

    private static final Logger log = LoggerFactory.getLogger(AlRegistrarseUnAsiento.class);

    @ApplicationModuleListener
    void alRegistrarse(AsientoRegistrado evento) {
        log.info("Apartado se entera del asiento {}: {}", evento.asientoId(), evento.descripcion());
    }
}
