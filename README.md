<div align="center">

# CIFRA

**Operación financiera autónoma para independientes colombianos y las empresas que les pagan**

[![CI](https://github.com/Santcar05/cifra/actions/workflows/ci.yml/badge.svg)](https://github.com/Santcar05/cifra/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25%20LTS-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![Spring Modulith](https://img.shields.io/badge/Spring%20Modulith-2.1-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)
![Astro](https://img.shields.io/badge/Astro-7-FF5D01?logo=astro&logoColor=white)
![Estado](https://img.shields.io/badge/Fase-0%20%C2%B7%20Tu%20propia%20herramienta-blue)

[Arquitectura](docs/CIFRA-arquitectura-y-diseno.md) ·
[Plan de implementación](docs/CIFRA-plan-de-implementacion.md) ·
[Serie de aprendizaje](docs/aprendizaje) ·
[Tutoriales](docs/tutoriales)

</div>

---

## Qué es CIFRA

Un desarrollador en Bogotá factura a un cliente en Medellín, a una agencia en Miami y a una startup en Barcelona. Cada factura tiene un régimen distinto, cada pago llega por un riel distinto con su propia comisión y su propio spread cambiario, y al final del mes la retención practicada, el IVA exento por exportación de servicios y el apartado para impuestos quedan repartidos entre una hoja de cálculo y la memoria.

**CIFRA cierra ese ciclo completo:** trabajo hecho → factura legalmente válida → cobro por cualquier riel → conciliación automática → impuestos apartados → declaración lista. Con agentes de IA que **proponen, nunca ejecutan a ciegas**.

| Lado | Quién | Qué obtiene |
|---|---|---|
| **B2C** | Independiente, freelancer, microempresa | Su operación financiera entera en piloto automático |
| **B2B** | Empresa que paga contratistas | Portal de facturas, validación, pago en lote, retenciones correctas |
| **B2B pro** | Contador / firma contable | Consola multi-cliente: 40 clientes en una pantalla en vez de 40 carpetas |

### El principio que define toda la arquitectura

> Un modelo de lenguaje nunca mueve dinero, nunca emite un documento fiscal y nunca envía un correo a un cliente por sí solo.

```
Agente (LLM + herramientas)
      │  emite Propuesta (nunca muta estado)
      ▼
Motor de reglas determinista
      │  valida: aritmética, normativa, límites, coherencia contable
      ▼
Escalera de autonomía
      │  ¿esta acción, en este monto, con esta confianza, está autorizada?
      ├── NO  → bandeja de aprobación humana
      └── SÍ  → ejecución + evento de auditoría inmutable
```

La aritmética de una factura la hace `BigDecimal`, no un transformer.

---

## Arquitectura: un monolito modular, no microservicios desde el día uno

Con una persona construyendo el producto y un solo usuario inicial, desplegar los once servicios que describe el documento de arquitectura sería cambiar tiempo de producto por tiempo de infraestructura. CIFRA arranca como **un solo despliegue** con **Spring Modulith**: fronteras entre módulos que el propio build hace cumplir, para que extraer un módulo a su propio servicio el día que haga falta sea mover un despliegue, no reescribir el sistema.

![Módulos del backend verificados por Spring Modulith](docs/images/FlowChart-SpringModulith.png)

```
cifra
├── shared        → Money, TasaDeCambio — tipos que usan todos los módulos
├── ledger        → asientos inmutables, saldos, reversiones
├── facturacion   → cuenta de cobro, numeración, TRM       (depende de: shared, ledger)
├── apartado      → el apartado fiscal                     (depende de: shared, ledger)
├── pagos         → rieles de pago, depósitos, idempotencia (depende de: shared)
├── conciliacion  → el conciliador determinista             (depende de: shared, ledger, pagos, facturacion)
├── sistema       → estado del sistema
└── seguridad     → andamio de autenticación
```

Las flechas solo van hacia abajo: `ledger` no sabe que `facturacion` existe. Si un módulo intenta usar las entrañas de otro, **el build falla** — no es una convención que alguien pueda olvidar, es una regla que `ArchUnit` y `ApplicationModules.verify()` comprueban en cada ejecución de pruebas.

---

## Stack tecnológico

| Capa | Tecnología | Por qué |
|---|---|---|
| **Backend** | Java 25 LTS · Spring Boot 4.1 · Spring Modulith 2.1 | Virtual threads sin programación reactiva; fronteras de módulo verificadas por el build |
| **Persistencia** | PostgreSQL 18 · Flyway | Todo lo que tiene consecuencia legal o monetaria vive aquí, con `numeric`, nunca `float` |
| **Eventos** | Spring Modulith Event Registry → Apache Kafka 4 (KRaft) | Los mismos eventos de dominio de hoy son los que saldrán por Kafka al extraer un servicio |
| **Caché / idempotencia** | Redis 8 | Que un doble clic o un webhook duplicado nunca produzca un cobro duplicado |
| **Landing pública** | Astro 7 · Tailwind CSS 4 | HTML estático, cero JavaScript salvo donde se pide explícitamente |
| **App (futura)** | Angular 22 | Signals, `resource()`, zoneless |
| **Infraestructura local** | Docker Compose | Postgres, Redis, Kafka y MinIO con un solo `docker compose up` |
| **Calidad** | ArchUnit · Testcontainers · JUnit 5 | Las reglas de arquitectura y las pruebas corren contra servicios reales, nunca contra una base en memoria |

El inventario completo, con cada decisión y su alternativa descartada, está en [`docs/aprendizaje/00-inventario-tecnologico.md`](docs/aprendizaje/00-inventario-tecnologico.md).

---

## Estado del proyecto

**Fase −1 (fundamentos)** — cerrada: entorno de desarrollo, infraestructura local, doble partida contable y `Money`/`BigDecimal` verificados.

**Fase 0 · Tu propia herramienta** — en curso. Diez épicas, criterio de salida: *usar CIFRA para la propia operación un mes completo sin volver a la hoja de cálculo.*

| Épica | Estado |
|---|:-:|
| Esqueleto del backend (Spring Boot + Modulith + Flyway) | ✅ Terminada |
| Persistencia y aislamiento (RLS, particionado, `numeric(19,4)`) | ⬜ Pendiente |
| Ledger event-sourced | 🔶 Andamio mínimo |
| Facturación interna | 🔶 Andamio mínimo |
| Rieles de pago e idempotencia | ⬜ Pendiente |
| Conciliador determinista + apartado fiscal | 🔶 Andamio mínimo |
| Eventos de dominio hacia Kafka | ⬜ Pendiente |
| App Angular mínima | ⬜ Pendiente |
| Pruebas con Testcontainers en CI | 🔶 Corre en local, falta el pipeline |

---

## Estructura del repositorio

```
CIFRA/
├── app/
│   ├── backend/          → Spring Boot + Modulith (Gradle, Kotlin DSL)
│   └── frontend/         → Angular (aún no iniciado)
├── landing-page/         → Astro 7 — la landing pública
├── infra/
│   └── docker-compose.yml → Postgres 18, Redis 8, Kafka 4, MinIO
├── docs/
│   ├── CIFRA-arquitectura-y-diseno.md
│   ├── CIFRA-plan-de-implementacion.md
│   ├── aprendizaje/      → serie numerada, de cero, cada comando verificado
│   └── tutoriales/       → tutoriales de estudio autocontenidos, uno por tema
├── diagrams/             → arquitectura en Excalidraw
└── tools/                → utilidades de generación de diagramas
```

---

## Cómo levantar el proyecto

**Requisitos:** Java 25 (Temurin), Docker Desktop, Node LTS + pnpm (solo para la landing).

```powershell
# 1. Infraestructura local (Postgres, Redis, Kafka, MinIO)
cd infra
docker compose up -d

# 2. Backend
cd ../app/backend
.\gradlew.bat test      # compila y corre toda la suite contra Postgres real
.\gradlew.bat bootRun   # arranca en :8080 (actuator en :8081)

# 3. Landing
cd ../../landing-page
pnpm install
pnpm dev                # http://localhost:4321
```

---

## Documentación

Este proyecto es, además de un producto, un **vehículo de aprendizaje deliberado**: cada pieza de la arquitectura tiene un documento que la enseña desde cero, con cada comando ejecutado de verdad contra infraestructura real antes de darlo por escrito.

- **[`docs/CIFRA-arquitectura-y-diseno.md`](docs/CIFRA-arquitectura-y-diseno.md)** — el qué y el porqué de todo el sistema.
- **[`docs/CIFRA-plan-de-implementacion.md`](docs/CIFRA-plan-de-implementacion.md)** — las seis fases, sus épicas y el criterio de "hecho" de cada una.
- **[`docs/aprendizaje/`](docs/aprendizaje)** — la serie numerada: entorno, infraestructura, contabilidad, Spring Boot, Modulith, Postgres avanzado, Git, DevOps.
- **[`docs/tutoriales/`](docs/tutoriales)** — un tutorial autocontenido por tema, de cero a poder defenderlo ante un entrevistador.

---

<div align="center">

Construido por [Santiago Castro](https://github.com/Santcar05) — en desarrollo activo, sin licencia definida todavía.

</div>
