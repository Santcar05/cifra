CREATE FUNCTION asegurar_particion(p_fecha DATE) RETURNS VOID
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = public
AS
$$
DECLARE
    desde  DATE := date_trunc('month', p_fecha)::date;
    hasta  DATE := (date_trunc('month', p_fecha) + INTERVAL '1 month')::date;
    sufijo TEXT := to_char(date_trunc('month', p_fecha), 'YYYY_MM');
BEGIN
    EXECUTE format('CREATE TABLE IF NOT EXISTS asiento_%s PARTITION OF asiento FOR VALUES FROM (%L) TO (%L)',
                   sufijo, desde, hasta);
    EXECUTE format('CREATE TABLE IF NOT EXISTS linea_asiento_%s PARTITION OF linea_asiento FOR VALUES FROM (%L) TO (%L)',
                   sufijo, desde, hasta);
END
$$;

REVOKE ALL ON FUNCTION asegurar_particion(DATE) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION asegurar_particion(DATE) TO cifra_app;

-- Particiones de enero de 2025 a diciembre de 2028
DO
$$
DECLARE
    mes DATE := DATE '2025-01-01';
BEGIN
    WHILE mes < DATE '2029-01-01'
        LOOP
            PERFORM asegurar_particion(mes);
            mes := (mes + INTERVAL '1 month')::date;
        END LOOP;
END
$$;
