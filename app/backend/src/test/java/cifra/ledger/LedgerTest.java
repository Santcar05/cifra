package cifra.ledger;

import static cifra.ledger.NuevaLinea.credito;
import static cifra.ledger.NuevaLinea.debito;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cifra.shared.Money;
import cifra.shared.persistencia.TenantSession;
import cifra.soporte.PostgresTestConfig;
import cifra.soporte.PostgresTestConfig.AdminDb;
import cifra.soporte.Tenants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestConfig.class)
class LedgerTest {

    @Autowired LedgerApi ledger;
    @Autowired TenantSession sesion;
    @Autowired AdminDb admin;

    UUID tenant;

    @BeforeEach
    void nuevoTenant() {
        tenant = Tenants.nuevo(admin);
    }

    private <T> T como(UUID t, Supplier<T> op) {
        return sesion.como(t, op);
    }

    private Asiento asentar(LocalDate fecha, String descripcion, NuevaLinea... lineas) {
        return como(tenant, () -> ledger.registrar(new NuevoAsiento(fecha, descripcion, List.of(lineas))));
    }

    private Money saldo(String cuenta, LocalDate hasta) {
        return como(tenant, () -> ledger.saldo(cuenta, hasta));
    }

    @Test
    void los_seis_hechos_del_documento_70_cuadran_y_dejan_los_saldos_esperados() {
        var fecha = LocalDate.of(2026, 9, 10);

        // 1. Factura emitida: $4.000.000
        asentar(fecha, "Factura 001",
                debito("1305", Money.cop("4000000")),
                credito("4135", Money.cop("4000000")));

        // 2 y 5. Pago recibido con retencion del 11% ya descontada
        asentar(fecha, "Pago factura 001 con retencion",
                debito("1110", Money.cop("3560000")),
                debito("1355", Money.cop("440000")),
                credito("1305", Money.cop("4000000")));

        // 3 y 4. Cobro internacional: USD 2.380 causados a TRM 4.050, recaudados a TRM 4.020,
        // con USD 39,82 de comision. La diferencia en cambio es lo que hace falta para que cuadre.
        var origenFactura = new OrigenExtranjero(Money.of("2380", "USD"), new BigDecimal("4050"));
        asentar(fecha, "Factura 002 en USD",
                debito("1305", Money.cop("9639000")).conOrigen(origenFactura),
                credito("4135", Money.cop("9639000")).conOrigen(origenFactura));
        asentar(fecha, "Cobro factura 002: Payoneer",
                debito("1110", Money.cop("9407523.60"))
                        .conOrigen(new OrigenExtranjero(Money.of("2340.18", "USD"), new BigDecimal("4020"))),
                debito("5305", Money.cop("160076.40"))
                        .conOrigen(new OrigenExtranjero(Money.of("39.82", "USD"), new BigDecimal("4020"))),
                debito("5315", Money.cop("71400.00")),
                credito("1305", Money.cop("9639000")));

        // 6. Apartado fiscal: mover plata del banco a la cuenta reservada. No es un gasto.
        asentar(fecha, "Apartado fiscal",
                debito("1120", Money.cop("600000")),
                credito("1110", Money.cop("600000")));

        assertThat(saldo("1305", fecha)).as("cuentas por cobrar: todo cobrado").isEqualTo(Money.cop("0"));
        assertThat(saldo("1110", fecha)).as("banco").isEqualTo(Money.cop("12367523.60"));
        assertThat(saldo("1355", fecha)).as("retencion por cobrar").isEqualTo(Money.cop("440000"));
        assertThat(saldo("1120", fecha)).as("apartado fiscal").isEqualTo(Money.cop("600000"));
        assertThat(saldo("4135", fecha)).as("ingresos").isEqualTo(Money.cop("13639000"));
        assertThat(saldo("5305", fecha)).as("comisiones").isEqualTo(Money.cop("160076.40"));
        assertThat(saldo("5315", fecha)).as("diferencia en cambio (gasto)").isEqualTo(Money.cop("71400"));

        // La ecuacion contable: activo = pasivo + patrimonio + (ingresos - gastos)
        var balance = como(tenant, () -> ledger.balanceDeComprobacion(fecha));
        var suma = (java.util.function.Function<TipoCuenta, BigDecimal>) tipo -> balance.stream()
                .filter(s -> s.tipo() == tipo).map(s -> s.saldo().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        var izquierda = suma.apply(TipoCuenta.ACTIVO);
        var derecha = suma.apply(TipoCuenta.PASIVO).add(suma.apply(TipoCuenta.PATRIMONIO))
                .add(suma.apply(TipoCuenta.INGRESO)).subtract(suma.apply(TipoCuenta.GASTO));
        assertThat(izquierda).isEqualByComparingTo(derecha);
        assertThat(izquierda).isEqualByComparingTo("13407523.60");
    }

    @Test
    void el_saldo_a_una_fecha_pasada_ignora_lo_que_paso_despues() {
        asentar(LocalDate.of(2026, 3, 10), "Marzo", debito("1110", Money.cop("100")), credito("4135", Money.cop("100")));
        asentar(LocalDate.of(2026, 4, 20), "Abril", debito("1110", Money.cop("50")), credito("4135", Money.cop("50")));

        assertThat(saldo("1110", LocalDate.of(2026, 3, 31))).isEqualTo(Money.cop("100"));
        assertThat(saldo("1110", LocalDate.of(2026, 4, 30))).isEqualTo(Money.cop("150"));
        assertThat(saldo("1110", LocalDate.of(2026, 2, 28))).isEqualTo(Money.cop("0"));
    }

    @Test
    void corregir_es_una_reversion_no_una_edicion() {
        var fecha = LocalDate.of(2026, 9, 10);
        var factura = asentar(fecha, "Factura mal hecha", debito("1305", Money.cop("500")), credito("4135", Money.cop("500")));

        var reversion = como(tenant, () -> ledger.revertir(factura.id(), fecha.plusDays(1), "monto equivocado"));

        assertThat(reversion.reversaDe()).isEqualTo(factura.id());
        assertThat(reversion.lineas()).extracting(Asiento.LineaContable::lado)
                .containsExactlyInAnyOrder(Lado.CREDITO, Lado.DEBITO);
        assertThat(saldo("1305", fecha.plusDays(1))).as("tras la reversion").isEqualTo(Money.cop("0"));
        assertThat(saldo("1305", fecha)).as("el original sigue ahi, a su fecha").isEqualTo(Money.cop("500"));
    }

    @Test
    void no_se_puede_revertir_dos_veces_ni_revertir_una_reversion() {
        var fecha = LocalDate.of(2026, 9, 10);
        var original = asentar(fecha, "X", debito("1110", Money.cop("10")), credito("4135", Money.cop("10")));
        var reversion = como(tenant, () -> ledger.revertir(original.id(), fecha, "prueba"));

        assertThatThrownBy(() -> como(tenant, () -> ledger.revertir(original.id(), fecha, "otra vez")))
                .hasMessageContaining("ya fue revertido");
        assertThatThrownBy(() -> como(tenant, () -> ledger.revertir(reversion.id(), fecha, "revertir la reversion")))
                .hasMessageContaining("No se revierte una reversion");
    }

    @Test
    void una_cuenta_que_no_existe_se_rechaza_con_un_mensaje_claro() {
        assertThatThrownBy(() -> asentar(LocalDate.of(2026, 9, 10), "Cuenta inventada",
                debito("9999", Money.cop("10")), credito("4135", Money.cop("10"))))
                .isInstanceOf(AsientoInvalidoException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void los_asientos_quedan_encadenados_y_la_cadena_se_verifica() {
        var fecha = LocalDate.of(2026, 9, 10);
        for (int i = 1; i <= 3; i++) {
            asentar(fecha, "Asiento " + i, debito("1110", Money.cop("10")), credito("4135", Money.cop("10")));
        }

        var verificacion = como(tenant, () -> ledger.verificarCadena());

        assertThat(verificacion.valida()).isTrue();
        assertThat(verificacion.asientosVerificados()).isEqualTo(3);
    }

    @Test
    void alterar_un_asiento_antiguo_rompe_la_cadena_aunque_se_desactive_el_trigger() {
        var fecha = LocalDate.of(2026, 3, 10);
        for (int i = 1; i <= 3; i++) {
            asentar(fecha, "Asiento " + i, debito("1110", Money.cop("10")), credito("4135", Money.cop("10")));
        }

        // Alguien con poder total sobre la base desactiva el trigger y reescribe el segundo asiento.
        var jdbc = admin.jdbc();
        jdbc.execute("ALTER TABLE asiento_2026_03 DISABLE TRIGGER asiento_inmutable");
        jdbc.update("UPDATE asiento SET descripcion = 'alterada' WHERE tenant_id = ? AND secuencia = 2", tenant);
        jdbc.execute("ALTER TABLE asiento_2026_03 ENABLE TRIGGER asiento_inmutable");

        var verificacion = como(tenant, () -> ledger.verificarCadena());

        assertThat(verificacion.valida()).isFalse();
        assertThat(verificacion.primeraSecuenciaInconsistente()).isEqualTo(2L);
        assertThat(verificacion.asientosVerificados()).isEqualTo(1);
    }

    @Test
    void asientos_simultaneos_del_mismo_tenant_reciben_secuencias_consecutivas_sin_huecos() throws Exception {
        var fecha = LocalDate.of(2026, 9, 10);
        var secuencias = new ArrayList<Long>();

        try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futuros = new ArrayList<java.util.concurrent.Future<Long>>();
            for (int i = 0; i < 20; i++) {
                int n = i;
                futuros.add(ejecutor.submit(() ->
                        asentar(fecha, "Concurrente " + n, debito("1110", Money.cop("10")), credito("4135", Money.cop("10")))
                                .secuencia()));
            }
            for (var futuro : futuros) {
                secuencias.add(futuro.get());
            }
        }

        assertThat(secuencias).doesNotHaveDuplicates().containsExactlyInAnyOrder(
                java.util.stream.LongStream.rangeClosed(1, 20).boxed().toArray(Long[]::new));
        assertThat(como(tenant, () -> ledger.verificarCadena()).valida()).isTrue();
    }

    @Test
    void un_tenant_no_ve_ni_afecta_el_ledger_de_otro() {
        var otro = Tenants.nuevo(admin);
        var fecha = LocalDate.of(2026, 9, 10);
        asentar(fecha, "Solo del primero", debito("1110", Money.cop("999")), credito("4135", Money.cop("999")));

        assertThat(como(otro, () -> ledger.saldo("1110", fecha))).isEqualTo(Money.cop("0"));
        assertThat(como(otro, () -> ledger.verificarCadena()).asientosVerificados()).isZero();
        assertThat(saldo("1110", fecha)).isEqualTo(Money.cop("999"));
    }

    @Test
    void sin_tenant_en_el_contexto_el_ledger_se_niega_a_trabajar() {
        assertThatThrownBy(() -> ledger.saldo("1110", LocalDate.of(2026, 9, 10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenant");
    }

    @Test
    void un_asiento_en_un_mes_sin_particion_la_crea_solo() {
        var fecha = LocalDate.of(2033, 2, 10);
        var antes = admin.jdbc().queryForObject("SELECT to_regclass('linea_asiento_2033_02') IS NOT NULL", Boolean.class);

        asentar(fecha, "Lejano futuro", debito("1110", Money.cop("10")), credito("4135", Money.cop("10")));

        var despues = admin.jdbc().queryForObject("SELECT to_regclass('linea_asiento_2033_02') IS NOT NULL", Boolean.class);
        assertThat(antes).isFalse();
        assertThat(despues).isTrue();
        assertThat(saldo("1110", fecha)).isEqualTo(Money.cop("10"));
    }
}
