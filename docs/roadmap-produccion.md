# KIDO — Roadmap para un lanzamiento real

KIDO ya tiene una base válida de producto: dominio de cursos, contenido, inscripción, progreso, asistencia, certificados, pagos, retiros, reembolsos, seguridad, mensajería y observabilidad. Sin embargo, el `compose.yml` actual es un entorno integral de desarrollo/demostración; no debe confundirse con una topología de producción pública.

## Prioridad P0 — antes de exponer usuarios reales

- Identidad y autorización por recurso: no confiar en `estudianteId` o `docenteId` recibidos del cliente. Deben derivarse del JWT y comprobar propiedad del recurso en cada operación.
- Validar que una inscripción gratuita solo pueda crearse para cursos realmente gratuitos y publicados.
- Webhook de Mercado Pago: validar autenticidad/firma, idempotencia y eventos repetidos antes de cambiar estado.
- Transactional Outbox para eventos que representan dinero, inscripción y certificados.
- Secretos fuera de Git/Compose: Secret Manager/Vault/variables protegidas; rotación de credenciales.
- TLS/HTTPS en el borde y Keycloak en modo producción, no `start-dev`.
- Backups automáticos, restauración probada y política de retención de PostgreSQL.
- Pruebas automáticas de reglas monetarias y estados antes de cada despliegue.

## Prioridad P1 — piloto controlado

- Rate limiting y protección de endpoints sensibles.
- Auditoría de acciones administrativas y financieras.
- Dead-letter topics / política de reintentos para consumidores Kafka.
- Correlation/trace ID uniforme en todos los servicios y eventos.
- Métricas de negocio: pagos aprobados/fallidos, compensaciones, reembolsos, retiros, cursos completados.
- Alertas: consumer lag, error rate, circuit breakers, DB pool y latencia de Mercado Pago.
- Almacenamiento de materiales en objeto (S3/MinIO compatible) con URLs firmadas y antivirus de carga.
- Separar el stack de observabilidad y las bases de datos de los contenedores de aplicación.

## Prioridad P2 — lanzamiento

- CI/CD con build, tests, análisis de dependencias, imagen inmutable y promoción por ambientes.
- Migraciones Flyway verificadas en staging antes de producción.
- Alta disponibilidad: Kafka y PostgreSQL administrados o replicados; múltiples instancias sin `container_name` fijo.
- Política de privacidad, términos, soporte, conciliación financiera y manejo de disputas.
- Pruebas de carga y recuperación ante fallos.

## Decisión arquitectónica S09

La activación de inscripción posterior a una compra se modela como Saga coreografiada. Esto reduce el acoplamiento síncrono entre Pago e Inscripción y permite compensar una venta cuando el acceso académico no puede crearse. Para pasar de una buena práctica académica a una garantía de producción, esta Saga debe complementarse con Outbox y DLQ/reintentos controlados.
