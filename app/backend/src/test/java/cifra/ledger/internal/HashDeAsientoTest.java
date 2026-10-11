package cifra.ledger.internal;

import static org.assertj.core.api.Assertions.assertThat;

import cifra.ledger.Lado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HashDeAsientoTest {

    static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final LocalDate FECHA = LocalDate.of(2026, 9, 10);

    private String hash(String anterior, String descripcion, List<String> lineas) {
        return HashDeAsiento.calcular(anterior, TENANT, 1, FECHA, descripcion, null, lineas);
    }

    @Test
    void el_mismo_contenido_da_siempre_el_mismo_hash() {
        var lineas = List.of("1305:DEBITO:4000000.0000", "4135:CREDITO:4000000.0000");
        assertThat(hash("GENESIS", "Factura", lineas)).isEqualTo(hash("GENESIS", "Factura", lineas));
    }

    @Test
    void el_orden_de_las_lineas_no_cambia_el_hash() {
        var a = List.of("1305:DEBITO:100.0000", "4135:CREDITO:100.0000");
        var b = List.of("4135:CREDITO:100.0000", "1305:DEBITO:100.0000");
        assertThat(hash("GENESIS", "x", a)).isEqualTo(hash("GENESIS", "x", b));
    }

    @Test
    void cambiar_un_solo_dato_cambia_el_hash() {
        var lineas = List.of("1305:DEBITO:100.0000", "4135:CREDITO:100.0000");
        var base = hash("GENESIS", "Factura", lineas);

        assertThat(hash("GENESIS", "Factura!", lineas)).isNotEqualTo(base);
        assertThat(hash("OTRO", "Factura", lineas)).isNotEqualTo(base);
        assertThat(hash("GENESIS", "Factura", List.of("1305:DEBITO:100.0001", "4135:CREDITO:100.0000")))
                .isNotEqualTo(base);
    }

    @Test
    void el_hash_incluye_el_del_anterior_asi_que_forma_una_cadena() {
        var lineas = List.of("1305:DEBITO:100.0000", "4135:CREDITO:100.0000");
        var primero = hash("GENESIS", "A", lineas);
        var segundoSobreElPrimero = hash(primero, "B", lineas);
        var segundoSobreOtro = hash("otro-hash", "B", lineas);

        assertThat(segundoSobreElPrimero).isNotEqualTo(segundoSobreOtro);
    }

    @Test
    void la_forma_canonica_de_una_linea_usa_siempre_cuatro_decimales() {
        assertThat(HashDeAsiento.linea("1110", Lado.DEBITO, new BigDecimal("4000000.5")))
                .isEqualTo("1110:DEBITO:4000000.5000");
        assertThat(HashDeAsiento.linea("1110", Lado.DEBITO, new BigDecimal("4000000.5000")))
                .isEqualTo("1110:DEBITO:4000000.5000");
    }

    @Test
    void el_hash_tiene_64_caracteres_hexadecimales() {
        assertThat(hash("GENESIS", "x", List.of("a:DEBITO:1.0000"))).matches("[0-9a-f]{64}");
    }
}
