# Postmortem 0001 · Tres tablas del ledger sin aislamiento entre tenants

| | |
|---|---|
| **Estado** | Cerrado |
| **Severidad** | Alta en potencia, sin impacto real (ver *Impacto*) |
| **Introducido** | 2026-09-29, commit `e01d79b` (migración `V4__ledger.sql`) |
| **Detectado** | 2026-10-11, al implementar el ledger (documento 33) |
| **Corregido** | 2026-10-11, commit `145d819` (migración `V7__aislamiento_del_ledger.sql`) |
| **Prevención** | `CoberturaRlsTest`, en el CI desde este cambio |

Este documento es **sin culpa**: describe cómo el sistema permitió el error, no quién lo cometió. La pregunta no es "¿quién se equivocó?" sino "¿por qué nada lo detuvo?".

## Resumen

La migración V4 activó Row Level Security en `cuenta`, pero **no** en `cadena_asientos`, `asiento` ni `linea_asiento`. Con eso, el rol de la aplicación (`cifra_app`) podía leer los asientos de **todos** los tenants. Durante 12 días esas tres tablas no tuvieron aislamiento y ninguna prueba falló.

## Impacto

**Ninguno real.** CIFRA está en la Fase 0, no hay producción, y la base solo tuvo datos de desarrollo y de pruebas, con un único tenant real (el propietario).

**En potencia, el peor fallo posible del producto.** Con varios tenants, cualquiera habría podido ver la contabilidad de otro. El aislamiento entre tenants es la garantía central de un sistema multi-tenant, y aquí faltaba justo en las tablas con datos financieros.

## Cronología

| Fecha | Hecho |
|---|---|
| 2026-09-29 | Se escribe la V4 siguiendo el documento 32. Su bloque de RLS trae el ejemplo para `cuenta` y, para las otras tres tablas, la línea `-- ... lo mismo para cadena_asientos, asiento y linea_asiento ...` |
| 2026-09-29 | La línea se copia tal cual. Es un comentario SQL válido, así que Flyway aplica la migración **sin ningún error** |
| 2026-09-29 → 10-11 | Las pruebas de la época pasan. Ninguna comprueba qué tablas tienen RLS |
| 2026-10-11 | Al implementar el documento 33 se leen las migraciones antes de ejecutar las pruebas, y aparece la línea de comentario donde debía haber SQL |
| 2026-10-11 | Se añade la V7 con `ENABLE` + `FORCE` y la política para las tres tablas. Pasan las 52 pruebas |
| 2026-10-13 | Se añade `CoberturaRlsTest`. Se verifica que, **retirando la V7**, la prueba falla y nombra exactamente `asiento`, `cadena_asientos` y `linea_asiento` |

## Causa raíz

**Un fallo seguro por defecto que no lo era.** Olvidar RLS no produce ningún error: la tabla funciona perfectamente, solo que sin filtro. Es un fallo **silencioso** por diseño de Postgres: RLS es algo que se activa, no algo que se desactiva.

Factores que contribuyeron:

1. **Un ejemplo abreviado con apariencia de código.** El documento 32 abreviaba con un comentario SQL dentro de un bloque de código. Al copiarlo, la abreviatura se volvió código válido que no hace nada.
2. **Ninguna comprobación automática.** El propio documento 32 proponía como ejercicio una prueba que verificara que *todas* las tablas con `tenant_id` tuvieran RLS. Nunca se escribió.
3. **Las pruebas de aislamiento comprobaban casos, no cobertura.** `AislamientoTest` demostraba que RLS funciona **donde está activado**. Ninguna prueba preguntaba **dónde falta**.

## Qué salió bien

- La migración aplicada **no se editó**. Cambiar la V4 habría roto el checksum de Flyway en toda base donde ya estuviera aplicada; la corrección fue una migración nueva (V7).
- El error apareció antes de tener un segundo tenant real.

## Acciones

| # | Acción | Tipo | Estado |
|---|---|---|---|
| 1 | Migración V7 con RLS para las tres tablas | Corrección | Hecho |
| 2 | `CoberturaRlsTest`: falla si una tabla con `tenant_id` no tiene `ENABLE` **y** `FORCE`, o si una tabla con RLS no tiene política | Prevención | Hecho |
| 3 | El backend se prueba en el CI (`ci.yml`, job `backend`): la prueba de la acción 2 bloquea cualquier pull request que la incumpla | Prevención | Hecho |
| 4 | El bloque de RLS del documento 32 se reescribe completo, sin abreviaturas dentro del código | Prevención | Hecho |

## Lección

**Una garantía que no se comprueba de forma automática no es una garantía, es una intención.** Las propiedades de seguridad que fallan en silencio (RLS, permisos, cifrado) necesitan una prueba que pregunte por su **cobertura**, no solo por su funcionamiento.
