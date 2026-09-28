package cifra.apartado;

import cifra.ledger.AsientoRegistrado;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class OyenteInestable {

    public static volatile boolean falla = false;

    @ApplicationModuleListener
    void alRegistrarse(AsientoRegistrado evento) {
        if (falla) {
            throw new IllegalStateException("fallo simulado del oyente");
        }
    }
}
