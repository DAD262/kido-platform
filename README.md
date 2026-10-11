# Kido Platform

Kido es una plataforma distribuida para cursos digitales gratuitos y pagados. Este repositorio conserva el avance de **Yana Rojas Adan** y continúa con el aporte de **Coila Mamani Jhoel Midwar** sobre la misma arquitectura.

## Estado actual

| Componente | Responsable | Estado |
|---|---|---|
| `kido-curso-ms` | Adan | Implementado |
| `kido-inscripcion-ms` | Adan | Implementado |
| `kido-pago-ms` | Midwar | Implementado |
| `kido-notificacion-ms` | Midwar | Implementado |
| `kido-config` | Compartido | Implementado |
| `kido-eureka` | Compartido | Implementado |
| `kido-gateway` | Compartido | Implementado |

La evaluación S5 pide un sistema distribuido base funcional con REST persistente, Config Server, Eureka, Gateway y múltiples instancias. Esa evidencia continúa intacta y el aporte de Midwar agrega la segunda mitad funcional del backend de negocio.

## Reglas de Kido implementadas

- Cursos gratuitos y pagados.
- Comisión de Kido: **10 %**.
- Neto del docente: **90 %**.
- Saldo docente pendiente: **7 días**.
- Retiro mínimo: **S/ 50**.
- Retiro por `CUENTA_BANCARIA`, `YAPE` o `PLIN`.
- Estados de retiro: `PENDING`, `APPROVED`, `PAID`, `REJECTED`.
- Reembolso: máximo 7 días y hasta 20 % de progreso.
- Pago aprobado: publica `pago.aprobado` en Kafka; Inscripción activa acceso con protección ante duplicados y devuelve confirmación asíncrona.
- Reembolso aprobado: revoca la inscripción y revierte el saldo de la venta.
- Mercado Pago sandbox: integración HTTP real cuando se configura un token `TEST-...`.
- Notificaciones internas y correo configurable por SMTP.

## Arranque completo con Docker

Requisitos: Docker Desktop, Docker Compose y Git.

```powershell
Copy-Item .env.example .env
notepad .env
docker compose up -d --build
docker compose ps
```

Como mínimo reemplaza `DB_PASS` en `.env`. El archivo `.env` está ignorado por Git.

Accesos principales:

- Gateway: http://localhost:18080
- Eureka: http://localhost:18761
- Config Server: http://localhost:18888/kido-pago-ms/prod
- Cursos: http://localhost:18080/api/v1/cursos
- Inscripciones: http://localhost:18080/api/v1/inscripciones
- Pagos: http://localhost:18080/api/v1/pagos/ordenes
- Retiros: http://localhost:18080/api/v1/retiros
- Reembolsos: http://localhost:18080/api/v1/reembolsos
- Notificaciones: http://localhost:18080/api/v1/notificaciones

## Mercado Pago sandbox

Para usar el flujo real agrega a `.env` un Access Token de prueba proporcionado por Mercado Pago:

```env
MERCADO_PAGO_ACCESS_TOKEN=TEST-xxxxxxxx
```

El token nunca debe subirse a GitHub. La aprobación simulada está **desactivada por defecto**. Para demos locales debes habilitarla explícitamente en `.env` y recrear los contenedores. En un despliegue real usa:

```env
KIDO_PAGOS_PERMITIR_SIMULACION=false
```

## Demo del aporte de Midwar

Con el stack iniciado:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-midwar.ps1
```

El script crea una orden para el curso pagado sembrado por Flyway, aprueba el pago en modo local, comprueba la inscripción creada, consulta el saldo pendiente del docente y revisa las notificaciones generadas.

## Prueba S5 original

La demostración de Unidad I de Adan sigue disponible:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-s5.ps1
```

## Estructura

```text
kido-platform/
├── infra/
│   ├── kido-config/
│   ├── kido-eureka/
│   └── kido-gateway/
├── services/
│   ├── kido-curso-ms/
│   ├── kido-inscripcion-ms/
│   ├── kido-pago-ms/
│   └── kido-notificacion-ms/
├── docs/
├── scripts/
├── compose.yml
└── mkdocs.yml
```


## S09 — Saga coreografiada (estado académico)

La S09 está implementada en el backend: idempotencia de compra, compensación de venta cuando no se puede activar una inscripción por una regla definitiva y reintentos seguros ante fallos transitorios. El circuito de notificación también evita duplicados de mensajes Kafka.

**Documentación y validación:** [`docs/s09-implementacion-validacion.md`](docs/s09-implementacion-validacion.md).

Para ejecutar las pruebas en **un laboratorio local, nunca con dinero real**, establece en `.env`:

```dotenv
KIDO_PAGOS_PERMITIR_SIMULACION=true
KIDO_SAGA_ESTUDIANTE_FALLO_DEMO=999999
```

Después:

```powershell
docker compose up -d --build
powershell -ExecutionPolicy Bypass -File .\scripts\verificar-s09.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\demo-s9-saga.ps1
```

Para un lanzamiento público **todavía hay controles P0 pendientes**, especialmente autorización por identidad JWT/propietario, Outbox transaccional, endurecimiento del webhook de Mercado Pago y despliegue/secretos/HTTPS. Ver [`docs/roadmap-produccion.md`](docs/roadmap-produccion.md).

## Documentación

- [Brief técnico](docs/brief-tecnico.md)
- [Producto Unidad I](docs/u1-producto.md)
- [Guía S5](docs/guia-s5.md)
- [Aporte de Midwar](docs/continuacion-midwar.md)
- [Avance técnico de Midwar](docs/avance-midwar.md)
- [Lista de microservicios](LISTA_MICROSERVICIOS.md)


## Seguridad Keycloak

La continuación incorpora Keycloak (realm `kido`) y JWT en el Gateway con roles `ADMIN`, `DOCENTE` y `ESTUDIANTE`. Ver `docs/seguridad-keycloak.md` y ejecutar `scripts/demo-security.ps1`.

## Tarea S08 - Kafka

Se añadió mensajería asíncrona con Kafka para el evento propio `pago.aprobado`: `kido-pago-ms` publica después del commit y `kido-notificacion-ms` consume con su propio grupo. Kafka UI queda en `http://localhost:18085`.

```powershell
docker compose up -d --build
powershell -ExecutionPolicy Bypass -File .\scripts\demo-s8.ps1
```

Para la evidencia de desacople:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-s8-desacople.ps1
```

La guía exacta de capturas y el contrato están en `docs/s08-mensajeria-asincrona.md`.


## Estado pre-frontend del backend

La base S08 se amplió para dejar la lógica de negocio preparada antes de iniciar interfaz. El backend ahora contempla:

- catálogo de cursos con flujo BORRADOR → PENDIENTE_REVISION → PUBLICADO/RECHAZADO;
- módulos, lecciones y materiales, incluyendo carga real de archivos;
- reglas configurables de progreso mínimo, asistencia mínima y certificado;
- inscripción gratuita y por compra;
- seguimiento de progreso y asistencia;
- evaluación automática de cumplimiento académico;
- certificados gratuitos o pagados, con PDF descargable;
- pagos de cursos y certificados mediante el dominio de Pago;
- comisión Kido/docente, saldos, retiros y reembolsos;
- Kafka para desacoplar eventos de pago y notificaciones;
- Keycloak/JWT, Gateway, Eureka, Config Server, PostgreSQL, Flyway, Prometheus y Grafana.

La documentación funcional agregada está en `docs/backend-funcional-completo.md` y el detalle de cambios en `CAMBIOS_BACKEND_COMPLETO.md`.

- eventos académicos Kafka para curso completado y certificado disponible.


## S09 — Saga coreografiada e idempotencia

La compra de cursos evoluciona a un flujo asíncrono entre `kido-pago-ms` y `kido-inscripcion-ms`. Pago publica `pago.aprobado`; Inscripción procesa de forma idempotente y responde con `inscripcion.activada` o `inscripcion.fallida`. Ante fallo, Pago compensa la venta y evita dobles compensaciones.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-s9-saga.ps1
```

Consulta [S09 — Saga KIDO](docs/s09-saga-kido.md).

## Camino a producción

El `compose.yml` de este repositorio sigue siendo un entorno integral de desarrollo/demostración. Los bloqueadores y prioridades para un lanzamiento real están documentados en [Roadmap de producción](docs/roadmap-produccion.md).
