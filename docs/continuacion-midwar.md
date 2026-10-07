# Aporte de Midwar

Midwar continuó directamente el repositorio dejado por Adan. No se creó una arquitectura paralela: se conservaron Config Server, Eureka, Gateway, Curso e Inscripción y se añadieron los dos microservicios que faltaban de la división acordada.

## Implementado por Midwar

### `kido-pago-ms`

- Órdenes de compra de cursos pagados.
- Consulta interna de Curso mediante nombre lógico de Eureka.
- Comisión Kido del 10 % y neto docente del 90 %.
- Saldo pendiente durante 7 días y liberación automática.
- Integración con Mercado Pago sandbox mediante la API HTTP y token `TEST-...` externo al repositorio.
- Endpoint de simulación habilitado solo para la ejecución local de demostración.
- Creación automática de la inscripción `COMPRA` después de aprobar un pago.
- Reintento explícito de sincronización si Inscripciones estuvo temporalmente no disponible.
- Retiros desde S/ 50 a cuenta bancaria, Yape o Plin con estados `PENDING`, `APPROVED`, `PAID`, `REJECTED`.
- Reembolsos durante 7 días cuando el progreso no supera 20 %.
- Revocación de acceso y reversión del movimiento de saldo al aprobar un reembolso.
- PostgreSQL, Flyway, Config Client, Eureka, Swagger DEV, Actuator y Prometheus.

### `kido-notificacion-ms`

- Notificaciones internas persistentes.
- Consulta por usuario y bandeja de no leídas.
- Marcado como leído.
- Canal `EMAIL` configurable por SMTP sin credenciales versionadas.
- En DEV el correo queda marcado como `SIMULADA`; las notificaciones `IN_APP` se entregan normalmente.
- Pago genera avisos de compra aprobada, nueva venta y reembolso.

## Cambios de integración necesarios

Se añadieron endpoints internos a Curso e Inscripción. Estos endpoints no se publican por Gateway y sirven exclusivamente para comunicación entre microservicios:

- `GET /internal/v1/cursos/{id}/resumen-compra`
- `POST /internal/v1/inscripciones/compra`
- `GET /internal/v1/inscripciones/por-estudiante-curso`
- `PUT /internal/v1/inscripciones/por-estudiante-curso/revocar`

También se agregaron las rutas públicas del Gateway para Pagos, Retiros, Reembolsos y Notificaciones, además de las configuraciones DEV/PROD de los dos nuevos servicios.

## Lo que no se mezcló con este aporte

Autenticación/JWT, Kafka, Resilience4j, Angular, Grafana y Loki corresponden a integración compartida de unidades posteriores. El código actual deja separados los límites de cada microservicio para poder incorporarlos sin reescribir la lógica de negocio.
