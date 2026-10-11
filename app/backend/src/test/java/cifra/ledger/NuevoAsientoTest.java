package cifra.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cifra.shared.Money;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NuevoAsientoTest {

    static final LocalDate HOY = LocalDate.of(2026, 9, 10);

    @Test
    void un_asiento_cuadrado_se_construye() {
        var asiento = new NuevoAsiento(HOY, "Factura", List.of(
                NuevaLinea.debito("1305", Money.cop("4000000")),
                NuevaLinea.credito("4135", Money.cop("4000000"))));

        assertThat(asiento.totalDebitos()).isEqualTo(Money.cop("4000000"));
    }

    @Test
    void un_asiento_descuadrado_no_se_puede_ni_construir() {
        assertThatThrownBy(() -> new NuevoAsiento(HOY, "Malo", List.of(
                NuevaLinea.debito("1305", Money.cop("4000000")),
                NuevaLinea.credito("4135", Money.cop("3999999.99")))))
                .isInstanceOf(AsientoInvalidoException.class)
                .hasMessageContaining("descuadrado");
    }

    @Test
    void un_asiento_con_una_sola_linea_es_invalido() {
        assertThatThrownBy(() -> new NuevoAsiento(HOY, "Medio asiento",
                List.of(NuevaLinea.debito("1305", Money.cop("100")))))
                .hasMessageContaining("al menos dos lineas");
    }

    @Test
    void un_importe_cero_o_negativo_es_invalido_el_signo_lo_da_el_lado() {
        assertThatThrownBy(() -> NuevaLinea.debito("1305", Money.cop("0")))
                .isInstanceOf(AsientoInvalidoException.class);
        assertThatThrownBy(() -> NuevaLinea.credito("1305", Money.cop("-5")))
                .isInstanceOf(AsientoInvalidoException.class);
    }

    @Test
    void el_ledger_solo_admite_pesos() {
        assertThatThrownBy(() -> new NuevoAsiento(HOY, "En dolares", List.of(
                NuevaLinea.debito("1110", Money.of("100", "USD")),
                NuevaLinea.credito("4135", Money.of("100", "USD")))))
                .hasMessageContaining("COP");
    }

    @Test
    void la_descripcion_es_obligatoria() {
        assertThatThrownBy(() -> new NuevoAsiento(HOY, "  ", List.of(
                NuevaLinea.debito("1305", Money.cop("1")), NuevaLinea.credito("4135", Money.cop("1")))))
                .hasMessageContaining("descripcion");
    }
}
