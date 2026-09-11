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
- Pago aprobado: crea inscripción `COMPRA` y habilita acceso.
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

El token nunca debe subirse a GitHub. Para la demo local, `compose.yml` habilita la aprobación simulada sin credenciales. En un despliegue real usa:

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

## Documentación

- [Brief técnico](docs/brief-tecnico.md)
- [Producto Unidad I](docs/u1-producto.md)
- [Guía S5](docs/guia-s5.md)
- [Aporte de Midwar](docs/continuacion-midwar.md)
- [Avance técnico de Midwar](docs/avance-midwar.md)
- [Lista de microservicios](LISTA_MICROSERVICIOS.md)


## Seguridad Keycloak

La continuación incorpora Keycloak (realm `kido`) y JWT en el Gateway con roles `ADMIN`, `DOCENTE` y `ESTUDIANTE`. Ver `docs/seguridad-keycloak.md` y ejecutar `scripts/demo-security.ps1`.
