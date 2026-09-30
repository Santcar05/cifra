CREATE TABLE cuenta
(
    id        UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    codigo    TEXT NOT NULL,
    nombre    TEXT NOT NULL,
    tipo      TEXT NOT NULL CHECK (tipo IN ('ACTIVO', 'PASIVO', 'PATRIMONIO', 'INGRESO', 'GASTO')),
    UNIQUE (tenant_id, codigo)
);

CREATE TABLE cadena_asientos
(
    tenant_id        UUID PRIMARY KEY REFERENCES tenant (id),
    ultima_secuencia BIGINT NOT NULL DEFAULT 0,
    ultimo_hash      TEXT   NOT NULL DEFAULT 'GENESIS'
);
CREATE TABLE asiento
(
    id            UUID          NOT NULL,
    tenant_id     UUID          NOT NULL REFERENCES tenant (id),
    secuencia     BIGINT        NOT NULL,
    fecha         DATE          NOT NULL,
    descripcion   TEXT          NOT NULL,
    reversa_de    UUID,
    hash_anterior TEXT          NOT NULL,
    hash          TEXT          NOT NULL,
    creado_en     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    PRIMARY KEY (id, fecha)
) PARTITION BY RANGE (fecha);

CREATE TABLE linea_asiento
(
    id             UUID           NOT NULL,
    asiento_id     UUID           NOT NULL,
    tenant_id      UUID           NOT NULL REFERENCES tenant (id),
    fecha          DATE           NOT NULL,
    cuenta_id      UUID           NOT NULL REFERENCES cuenta (id),
    lado           TEXT           NOT NULL CHECK (lado IN ('DEBITO', 'CREDITO')),
    importe        NUMERIC(19, 4) NOT NULL CHECK (importe > 0),
    moneda_origen  CHAR(3),
    importe_origen NUMERIC(19, 4),
    trm            NUMERIC(19, 6),
    PRIMARY KEY (id, fecha),
    FOREIGN KEY (asiento_id, fecha) REFERENCES asiento (id, fecha)
) PARTITION BY RANGE (fecha);

CREATE INDEX asiento_tenant_secuencia_idx ON asiento (tenant_id, secuencia);
CREATE INDEX linea_asiento_saldo_idx ON linea_asiento (tenant_id, cuenta_id, fecha);
CREATE INDEX linea_asiento_asiento_idx ON linea_asiento (asiento_id, fecha);

---
CREATE FUNCTION rechazar_modificacion() RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    RAISE EXCEPTION 'El ledger es inmutable: % sobre % no esta permitido. Corrija con un asiento de reversion.',
        TG_OP, TG_TABLE_NAME
        USING ERRCODE = '55000';
END
$$;

CREATE TRIGGER asiento_inmutable
    BEFORE UPDATE OR DELETE ON asiento
    FOR EACH ROW EXECUTE FUNCTION rechazar_modificacion();

CREATE TRIGGER linea_asiento_inmutable
    BEFORE UPDATE OR DELETE ON linea_asiento
    FOR EACH ROW EXECUTE FUNCTION rechazar_modificacion();

CREATE TRIGGER asiento_sin_truncate
    BEFORE TRUNCATE ON asiento
    FOR EACH STATEMENT EXECUTE FUNCTION rechazar_modificacion();

CREATE TRIGGER linea_asiento_sin_truncate
    BEFORE TRUNCATE ON linea_asiento
    FOR EACH STATEMENT EXECUTE FUNCTION rechazar_modificacion();
---
GRANT SELECT, INSERT ON cuenta, asiento, linea_asiento TO cifra_app;
GRANT SELECT, INSERT, UPDATE ON cadena_asientos TO cifra_app;

---
CREATE FUNCTION verificar_asiento_cuadrado() RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    debitos  NUMERIC(19, 4);
    creditos NUMERIC(19, 4);
BEGIN
    SELECT COALESCE(SUM(importe) FILTER (WHERE lado = 'DEBITO'), 0),
           COALESCE(SUM(importe) FILTER (WHERE lado = 'CREDITO'), 0)
    INTO debitos, creditos
    FROM linea_asiento
    WHERE asiento_id = NEW.asiento_id
      AND fecha = NEW.fecha;

    IF debitos <> creditos THEN
        RAISE EXCEPTION 'Asiento % descuadrado: debitos=% creditos=%', NEW.asiento_id, debitos, creditos
            USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END
$$;

CREATE CONSTRAINT TRIGGER asiento_cuadrado
    AFTER INSERT ON linea_asiento
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION verificar_asiento_cuadrado();

---
ALTER TABLE cuenta ENABLE ROW LEVEL SECURITY;
ALTER TABLE cuenta FORCE ROW LEVEL SECURITY;
-- ... lo mismo para cadena_asientos, asiento y linea_asiento ...

CREATE POLICY aislamiento_tenant ON cuenta
    USING (tenant_id = app_tenant_id()) WITH CHECK (tenant_id = app_tenant_id());
-- ... lo mismo para cadena_asientos, asiento y linea_asiento ...
