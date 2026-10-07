# Cambios S08 - Mensajería asíncrona

## Infraestructura

- Se agregó Apache Kafka en modo KRaft al `compose.yml`.
- Se agregó Kafka UI en `http://localhost:18085`.
- Kafka queda disponible como `kido-kafka:9092` dentro de Docker y `localhost:19092` desde el host.
- Se creó el topic `kido-pago-eventos` con 3 particiones y factor de réplica 1.

## Evento propio de Kido

- Evento: `pago.aprobado`.
- Productor: `kido-pago-ms`.
- Consumidor: `kido-notificacion-ms`.
- Key: `ordenId`.
- Consumer group: `kido-notificacion-ms`.
- El productor publica con `afterCommit`, evitando anunciar un pago que no haya quedado confirmado en base de datos.

## Desacople

El flujo de aprobación de pago dejó de invocar directamente a `kido-notificacion-ms`. El pago se confirma, se publica el evento en Kafka y el servicio de notificaciones lo procesa cuando está disponible. Si el consumidor se apaga, el pago sigue quedando `APROBADA` y el evento espera en Kafka.

## Evidencia

- `scripts/demo-s8.ps1`: flujo normal.
- `scripts/demo-s8-desacople.ps1`: prueba de consumidor apagado, lag pendiente y consumo al volver.
- `docs/s08-mensajeria-asincrona.md`: contrato, pasos de ejecución y lista de capturas.
- `INSTRUCCIONES_S08.txt`: guía rápida para levantar y capturar.
