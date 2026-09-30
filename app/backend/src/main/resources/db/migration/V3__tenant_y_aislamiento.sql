CREATE TABLE tenant
(
    id           UUID PRIMARY KEY,
    nombre       TEXT        NOT NULL,
    moneda_base  CHAR(3)     NOT NULL DEFAULT 'COP',
    zona_horaria TEXT        NOT NULL DEFAULT 'America/Bogota',
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- El unico tenant de la Fase 0: el propietario.
INSERT INTO tenant (id, nombre) VALUES ('00000000-0000-0000-0000-000000000001', 'Propietario');

-- Devuelve el tenant de la transaccion actual, o NULL si no se fijo ninguno.
-- current_setting(..., true) no falla si la variable no existe: devuelve NULL.
CREATE FUNCTION app_tenant_id() RETURNS UUID
    LANGUAGE sql
    STABLE
AS
$$
SELECT NULLIF(current_setting('app.tenant_id', true), '')::uuid
$$;

ALTER TABLE tenant ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant FORCE ROW LEVEL SECURITY;

CREATE POLICY aislamiento_tenant ON tenant
    USING (id = app_tenant_id());

GRANT SELECT ON tenant TO cifra_app;
