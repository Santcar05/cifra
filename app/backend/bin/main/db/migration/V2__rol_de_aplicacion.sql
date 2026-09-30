-- La aplicacion se conecta con un rol SIN privilegios de propietario ni superusuario.
-- Solo asi las politicas de RLS le aplican (el propietario de una tabla y los superusuarios las ignoran).
-- La contrasena viene de un placeholder de Flyway; no vive en este archivo.
DO
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'cifra_app') THEN
        CREATE ROLE cifra_app LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
    END IF;
END
$$;

ALTER ROLE cifra_app WITH PASSWORD '${app_password}';

GRANT USAGE ON SCHEMA public TO cifra_app;

-- El registro de eventos de Modulith se lee y se actualiza al completar cada publicacion.
GRANT SELECT, INSERT, UPDATE, DELETE ON event_publication TO cifra_app;
