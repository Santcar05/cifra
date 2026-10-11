-- Prepara un tenant para llevar contabilidad: su plan de cuentas minimo y la cabecera de su cadena.
-- Los codigos siguen la logica del PUC colombiano (1 activo, 2 pasivo, 4 ingresos, 5 gastos),
-- simplificados para CIFRA. Revisalos con tu contador antes de presentar nada.
CREATE FUNCTION sembrar_plan_de_cuentas(p_tenant UUID) RETURNS VOID
    LANGUAGE plpgsql
AS
$$
BEGIN
    -- Con FORCE ROW LEVEL SECURITY hasta el propietario necesita un tenant en la transaccion.
    PERFORM set_config('app.tenant_id', p_tenant::text, true);

    INSERT INTO cuenta (id, tenant_id, codigo, nombre, tipo)
    VALUES (gen_random_uuid(), p_tenant, '1110', 'Banco', 'ACTIVO'),
           (gen_random_uuid(), p_tenant, '1120', 'Apartado fiscal', 'ACTIVO'),
           (gen_random_uuid(), p_tenant, '1305', 'Cuentas por cobrar', 'ACTIVO'),
           (gen_random_uuid(), p_tenant, '1355', 'Retencion en la fuente por cobrar', 'ACTIVO'),
           (gen_random_uuid(), p_tenant, '2404', 'Impuestos por pagar', 'PASIVO'),
           (gen_random_uuid(), p_tenant, '3115', 'Patrimonio del titular', 'PATRIMONIO'),
           (gen_random_uuid(), p_tenant, '4135', 'Ingresos por servicios', 'INGRESO'),
           (gen_random_uuid(), p_tenant, '4250', 'Diferencia en cambio (ingreso)', 'INGRESO'),
           (gen_random_uuid(), p_tenant, '5305', 'Comisiones bancarias y de pasarela', 'GASTO'),
           (gen_random_uuid(), p_tenant, '5315', 'Diferencia en cambio (gasto)', 'GASTO')
    ON CONFLICT (tenant_id, codigo) DO NOTHING;

    INSERT INTO cadena_asientos (tenant_id) VALUES (p_tenant) ON CONFLICT DO NOTHING;
END
$$;

SELECT sembrar_plan_de_cuentas('00000000-0000-0000-0000-000000000001');
