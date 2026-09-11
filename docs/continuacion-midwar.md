# Continuación de Midwar y del equipo

Este archivo separa el avance actual de las siguientes unidades.

## Responsabilidad principal de Midwar

- Crear `kido-pago-ms` con OrdenCompra y DetalleOrden.
- Integrar Mercado Pago sandbox mediante una llamada real.
- Registrar comisión, tarifa, saldo pendiente y saldo disponible.
- Implementar solicitudes de retiro y reembolsos.
- Crear `kido-notificacion-ms` y consumir eventos.

## Responsabilidad compartida

- Crear `kido-auth-ms` con Spring Security, JWT y roles.
- Proteger cada microservicio, no solo Gateway.
- Conectar Curso, Inscripción y Pago con OpenFeign y Resilience4j.
- Publicar y consumir eventos con Kafka.
- Asegurar idempotencia en pago, inscripción, reembolso y notificación.
- Añadir Prometheus, Grafana y Loki.
- Integrar Angular exclusivamente mediante Gateway.
- Completar certificados al finalizar el curso y decidir si se cobra una tarifa adicional.

## Regla de integración futura

La ruta HTTP de Inscripciones no concederá acceso de tipo COMPRA. Pago publicará un evento de pago aprobado; Inscripciones lo procesará una sola vez y creará el acceso. Un reembolso aprobado revocará el acceso mediante otro evento.
