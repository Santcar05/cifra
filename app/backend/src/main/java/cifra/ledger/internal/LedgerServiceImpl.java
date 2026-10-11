package cifra.ledger.internal;

import cifra.ledger.Asiento;
import cifra.ledger.Asiento.LineaContable;
import cifra.ledger.AsientoInvalidoException;
import cifra.ledger.AsientoRegistrado;
import cifra.ledger.Lado;
import cifra.ledger.LedgerApi;
import cifra.ledger.NuevaLinea;
import cifra.ledger.NuevoAsiento;
import cifra.ledger.SaldoDeCuenta;
import cifra.ledger.VerificacionDeCadena;
import cifra.shared.Money;
import cifra.shared.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LedgerServiceImpl implements LedgerApi {

    @PersistenceContext
    private EntityManager em;

    private final ApplicationEventPublisher eventos;
    private final ParticionesLedger particiones;

    LedgerServiceImpl(ApplicationEventPublisher eventos, ParticionesLedger particiones) {
        this.eventos = eventos;
        this.particiones = particiones;
    }

    @Override
    @Transactional
    public Asiento registrar(NuevoAsiento nuevo) {
        return registrar(nuevo, null);
    }

    private Asiento registrar(NuevoAsiento nuevo, UUID reversaDe) {
        var tenant = TenantContext.requerido();
        var cuentas = cuentasPorCodigo();
        for (var linea : nuevo.lineas()) {
            if (!cuentas.containsKey(linea.cuenta())) {
                throw new AsientoInvalidoException("La cuenta " + linea.cuenta() + " no existe en el plan de cuentas");
            }
        }
        particiones.asegurar(nuevo.fecha());

        // Bloquea la cabecera de la cadena: dos asientos del mismo tenant nunca se encadenan a la vez.
        var cadena = em.find(CadenaEntity.class, tenant, LockModeType.PESSIMISTIC_WRITE);
        if (cadena == null) {
            throw new IllegalStateException("El tenant " + tenant + " no tiene cadena de asientos: falta sembrar su plan de cuentas");
        }
        var secuencia = cadena.ultimaSecuencia() + 1;
        var canonicas = nuevo.lineas().stream()
                .map(l -> HashDeAsiento.linea(l.cuenta(), l.lado(), l.importe().amount()))
                .toList();
        var hash = HashDeAsiento.calcular(cadena.ultimoHash(), tenant, secuencia, nuevo.fecha(),
                nuevo.descripcion(), reversaDe, canonicas);

        var id = UUID.randomUUID();
        em.persist(new AsientoEntity(id, nuevo.fecha(), tenant, secuencia, nuevo.descripcion(),
                reversaDe, cadena.ultimoHash(), hash));
        var vistas = new ArrayList<LineaContable>();
        for (var linea : nuevo.lineas()) {
            var cuenta = cuentas.get(linea.cuenta());
            var origen = linea.origen();
            em.persist(new LineaAsientoEntity(UUID.randomUUID(), nuevo.fecha(), id, tenant, cuenta.id(), linea.lado(),
                    linea.importe().amount(),
                    origen == null ? null : origen.importeOrigen().currency().getCurrencyCode(),
                    origen == null ? null : origen.importeOrigen().amount(),
                    origen == null ? null : origen.trm()));
            vistas.add(new LineaContable(cuenta.codigo(), cuenta.nombre(), linea.lado(), linea.importe()));
        }
        cadena.avanzar(secuencia, hash);

        eventos.publishEvent(new AsientoRegistrado(tenant, id, secuencia, nuevo.fecha(), nuevo.descripcion()));
        return new Asiento(id, secuencia, nuevo.fecha(), nuevo.descripcion(), reversaDe, hash, vistas);
    }

    @Override
    @Transactional
    public Asiento revertir(UUID asientoId, LocalDate fecha, String motivo) {
        TenantContext.requerido();
        var original = em.createQuery("select a from AsientoEntity a where a.id = :id", AsientoEntity.class)
                .setParameter("id", asientoId)
                .getResultStream().findFirst()
                .orElseThrow(() -> new AsientoInvalidoException("No existe el asiento " + asientoId));
        if (original.reversaDe() != null) {
            throw new AsientoInvalidoException("No se revierte una reversion: registre el asiento correcto");
        }
        if (fecha.isBefore(original.fecha())) {
            throw new AsientoInvalidoException("La reversion no puede ser anterior al asiento que corrige");
        }
        var yaRevertido = em.createQuery("select count(a) from AsientoEntity a where a.reversaDe = :id", Long.class)
                .setParameter("id", asientoId).getSingleResult();
        if (yaRevertido > 0) {
            throw new AsientoInvalidoException("El asiento " + asientoId + " ya fue revertido");
        }

        var porId = cuentasPorCodigo().values().stream().collect(Collectors.toMap(CuentaEntity::id, Function.identity()));
        var invertidas = lineasDe(original).stream()
                .map(l -> new NuevaLinea(porId.get(l.cuentaId()).codigo(), l.lado().opuesto(),
                        new Money(l.importe(), MONEDA_FUNCIONAL), null))
                .toList();
        var descripcion = "Reversion del asiento " + original.secuencia() + ": " + motivo;
        return registrar(new NuevoAsiento(fecha, descripcion, invertidas), original.id());
    }

    @Override
    @Transactional(readOnly = true)
    public Money saldo(String codigoCuenta, LocalDate hasta) {
        TenantContext.requerido();
        var cuenta = cuentasPorCodigo().get(codigoCuenta);
        if (cuenta == null) {
            throw new AsientoInvalidoException("La cuenta " + codigoCuenta + " no existe en el plan de cuentas");
        }
        var neto = em.createQuery("""
                        select coalesce(sum(case when l.lado = :debito then l.importe else -l.importe end), 0)
                        from LineaAsientoEntity l
                        where l.cuentaId = :cuenta and l.fecha <= :hasta
                        """, BigDecimal.class)
                .setParameter("debito", Lado.DEBITO)
                .setParameter("cuenta", cuenta.id())
                .setParameter("hasta", hasta)
                .getSingleResult();
        return new Money(conSigno(neto, cuenta), MONEDA_FUNCIONAL);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaldoDeCuenta> balanceDeComprobacion(LocalDate hasta) {
        TenantContext.requerido();
        var netos = new HashMap<UUID, BigDecimal>();
        em.createQuery("""
                        select l.cuentaId, sum(case when l.lado = :debito then l.importe else -l.importe end)
                        from LineaAsientoEntity l
                        where l.fecha <= :hasta
                        group by l.cuentaId
                        """, Object[].class)
                .setParameter("debito", Lado.DEBITO)
                .setParameter("hasta", hasta)
                .getResultList()
                .forEach(fila -> netos.put((UUID) fila[0], (BigDecimal) fila[1]));

        return cuentasPorCodigo().values().stream()
                .sorted((a, b) -> a.codigo().compareTo(b.codigo()))
                .map(c -> new SaldoDeCuenta(c.codigo(), c.nombre(), c.tipo(),
                        new Money(conSigno(netos.getOrDefault(c.id(), BigDecimal.ZERO), c), MONEDA_FUNCIONAL)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VerificacionDeCadena verificarCadena() {
        var porId = cuentasPorCodigo().values().stream().collect(Collectors.toMap(CuentaEntity::id, Function.identity()));
        var lineas = em.createQuery("select l from LineaAsientoEntity l", LineaAsientoEntity.class).getResultList()
                .stream().collect(Collectors.groupingBy(LineaAsientoEntity::asientoId));
        var asientos = em.createQuery("select a from AsientoEntity a order by a.secuencia", AsientoEntity.class)
                .getResultList();
        var tenant = TenantContext.requerido();

        var hashPrevio = HashDeAsiento.GENESIS;
        var esperada = 1L;
        for (var asiento : asientos) {
            var canonicas = lineas.getOrDefault(asiento.id(), List.of()).stream()
                    .map(l -> HashDeAsiento.linea(porId.get(l.cuentaId()).codigo(), l.lado(), l.importe()))
                    .toList();
            var recalculado = HashDeAsiento.calcular(asiento.hashAnterior(), tenant, asiento.secuencia(),
                    asiento.fecha(), asiento.descripcion(), asiento.reversaDe(), canonicas);
            var consistente = asiento.secuencia() == esperada
                    && asiento.hashAnterior().equals(hashPrevio)
                    && asiento.hash().equals(recalculado);
            if (!consistente) {
                return new VerificacionDeCadena(false, esperada - 1, asiento.secuencia());
            }
            hashPrevio = asiento.hash();
            esperada++;
        }
        return new VerificacionDeCadena(true, asientos.size(), null);
    }

    private static BigDecimal conSigno(BigDecimal neto, CuentaEntity cuenta) {
        return cuenta.tipo().esDeudora() ? neto : neto.negate();
    }

    private Map<String, CuentaEntity> cuentasPorCodigo() {
        return em.createQuery("select c from CuentaEntity c", CuentaEntity.class).getResultList().stream()
                .collect(Collectors.toMap(CuentaEntity::codigo, Function.identity()));
    }

    private List<LineaAsientoEntity> lineasDe(AsientoEntity asiento) {
        return em.createQuery("select l from LineaAsientoEntity l where l.asientoId = :id and l.fecha = :fecha",
                        LineaAsientoEntity.class)
                .setParameter("id", asiento.id())
                .setParameter("fecha", asiento.fecha())
                .getResultList();
    }
}
