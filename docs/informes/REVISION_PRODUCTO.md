# Revisión de producto — KIDO

## Estado

KIDO ya es más que una maqueta CRUD: tiene catálogo y contenido, inscripción, progreso/asistencia, certificados, pagos, comisión y saldo docente, retiros, reembolsos, seguridad JWT/Keycloak, descubrimiento/configuración, Kafka y observabilidad. La versión actual incorpora además una Saga coreografiada para la activación de una compra.

## Cambio S09 aplicado

- `kido-pago-ms` publica `pago.aprobado` con un snapshot mínimo del curso.
- `kido-inscripcion-ms` consume el evento y crea la inscripción sin llamada síncrona desde Pago.
- La idempotencia usa revisión explícita y las restricciones UNIQUE de PostgreSQL.
- Inscripción publica `inscripcion.activada` o `inscripcion.fallida`.
- Pago consume el resultado; confirma sincronización o compensa la venta.
- La compensación revierte el movimiento del docente y, para un pago real, solicita reembolso a Mercado Pago con clave de idempotencia.
- Existe un gatillo determinista SOLO DEV (`estudianteId=999999`) para demostrar el camino de compensación de S09.

## Evaluación de lanzamiento

**Apto para demo académica/piloto técnico:** sí.

**Apto hoy para abrirlo a pagos reales de usuarios desconocidos en Internet:** todavía no. Los puntos P0 están en `docs/roadmap-produccion.md`.

Los tres más importantes son:

1. identidad por JWT y autorización por propietario del recurso (no confiar en IDs que manda el navegador);
2. Transactional Outbox + reintentos/DLQ para eventos financieros;
3. despliegue real con HTTPS, secretos administrados, backups y Keycloak/PostgreSQL/Kafka fuera del modo de desarrollo local.

## Validación realizada en esta revisión

- Estructura de los 7 proyectos Maven revisada.
- YAML de configuración validado sintácticamente.
- XML de los `pom.xml` validado sintácticamente.
- No fue posible ejecutar Maven dentro del entorno de revisión porque el wrapper necesita descargar Maven/dependencias desde Internet. La validación final debe hacerse en la PC de desarrollo con `docker compose up -d --build` y `scripts/demo-s9-saga.ps1` antes de subir a GitHub.
