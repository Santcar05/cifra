package cifra.sistema;

import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{version}/sistema")
class EstadoController {

    record Estado(String aplicacion, String hilo, Instant ahora) {}

    @GetMapping(path = "/estado", version = "1")
    Estado estado() {
        return new Estado("cifra-backend", Thread.currentThread().toString(), Instant.now());
    }
}
