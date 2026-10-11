-- La V4 solo activo RLS sobre cuenta: el "lo mismo para cadena_asientos, asiento y linea_asiento"
-- quedo como comentario. Una migracion ya aplicada no se edita (Flyway compara su checksum),
-- asi que el aislamiento de las tres tablas restantes se completa aqui.
ALTER TABLE cadena_asientos ENABLE ROW LEVEL SECURITY;
ALTER TABLE cadena_asientos FORCE ROW LEVEL SECURITY;
ALTER TABLE asiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE asiento FORCE ROW LEVEL SECURITY;
ALTER TABLE linea_asiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE linea_asiento FORCE ROW LEVEL SECURITY;

CREATE POLICY aislamiento_tenant ON cadena_asientos
    USING (tenant_id = app_tenant_id()) WITH CHECK (tenant_id = app_tenant_id());
CREATE POLICY aislamiento_tenant ON asiento
    USING (tenant_id = app_tenant_id()) WITH CHECK (tenant_id = app_tenant_id());
CREATE POLICY aislamiento_tenant ON linea_asiento
    USING (tenant_id = app_tenant_id()) WITH CHECK (tenant_id = app_tenant_id());
