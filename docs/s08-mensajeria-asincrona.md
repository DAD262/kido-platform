# S08 · Mensajería asíncrona en Kido

## Evento propio implementado

Kido implementa el evento de negocio **`pago.aprobado`**. El dueño del hecho es `kido-pago-ms`: cuando una orden de compra queda `APROBADA`, el servicio guarda sus cambios y **después del commit** publica el evento en Kafka. `kido-notificacion-ms` lo consume con su propio `group-id` y genera dos notificaciones: una para el estudiante y otra para el docente. No existe llamada HTTP entre estos dos servicios para este flujo.

- Topic: `kido-pago-eventos`
- Productor: `kido-pago-ms`
- Consumidor: `kido-notificacion-ms`
- Consumer group: `kido-notificacion-ms`
- Key: `ordenId`
- Particiones: `3`
- Evento: `pago.aprobado`

## Contrato del evento

```json
{
  "tipoEvento": "pago.aprobado",
  "ordenId": 12,
  "estudianteId": 1001,
  "docenteId": 1,
  "cursoId": 2,
  "cursoTitulo": "Curso de ejemplo",
  "montoTotal": 100.00,
  "montoDocente": 90.00,
  "diasRetencion": 7,
  "estado": "APROBADA",
  "origen": "kido-pago-ms",
  "timestamp": 1790000000000
}
```

| Campo | Tipo | Significado |
|---|---|---|
| `tipoEvento` | string | Siempre `pago.aprobado`. |
| `ordenId` | number | Identificador de la orden; también es la key de Kafka. |
| `estudianteId` | number | Usuario que realizó la compra. |
| `docenteId` | number | Dueño del curso vendido. |
| `cursoId` | number | Curso comprado. |
| `cursoTitulo` | string | Texto necesario para la notificación. |
| `montoTotal` | number | Importe total pagado. |
| `montoDocente` | number | Neto del docente después de la comisión de Kido. |
| `diasRetencion` | number | Días hasta liberar el saldo del docente. |
| `estado` | string | Estado de la orden al publicar el evento (`APROBADA`). |
| `origen` | string | Servicio que publicó el evento. |
| `timestamp` | number | Momento de publicación en milisegundos epoch. |

## Cómo levantar S08

Desde la raíz del proyecto en PowerShell:

```powershell
Copy-Item .env.example .env -ErrorAction SilentlyContinue
docker compose up -d --build
docker compose ps
```

Abrir:

- Kafka UI: `http://localhost:18085`
- Eureka: `http://localhost:18761`
- Gateway: `http://localhost:18080`
- Keycloak: `http://localhost:19090`

En Kafka UI debe aparecer el topic `kido-pago-eventos` con **3 particiones**. En Eureka deben aparecer `KIDO-PAGO-MS` y `KIDO-NOTIFICACION-MS`.

## Flujo normal y evidencia

Ejecuta:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-s8.ps1
```

El script crea una compra, la aprueba y muestra la orden y las notificaciones. Para evidenciar los logs:

```powershell
docker compose logs --tail=100 kido-pago-ms | Select-String "component=producer|pago.aprobado"
docker compose logs --tail=100 kido-notificacion-ms | Select-String "component=consumer|component=processor|pago.aprobado"
```

En Kafka UI entra a **Topics → kido-pago-eventos → Messages** y captura el JSON del evento con su `key`, `partition` y `offset`. En **Consumers** verifica el grupo `kido-notificacion-ms` con lag `0`.

## Prueba de desacople

Ejecuta:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-s8-desacople.ps1
```

El script detiene `kido-notificacion-ms`, aprueba una nueva compra y deja el consumidor apagado para que puedas capturar el evento pendiente y el lag. Después de tomar la captura, presiona ENTER: el servicio vuelve a iniciar y consume el evento que estaba esperando.

Para ver el lag por consola mientras el consumidor está apagado:

```powershell
docker exec kido-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kido-kafka:9092 --group kido-notificacion-ms --describe
```

## Capturas mínimas para el PDF

1. `docker compose ps` con Kafka, Kafka UI y microservicios levantados.
2. Kafka UI con `kido-pago-eventos` y sus 3 particiones.
3. Eureka con `KIDO-PAGO-MS` y `KIDO-NOTIFICACION-MS`.
4. Log del productor con `eventType=pago.aprobado ... status=published`.
5. Mensaje JSON en Kafka UI con key = `ordenId`.
6. Log del consumidor con `status=consumed` y `status=processed`.
7. Notificaciones creadas para estudiante y docente.
8. Prueba de desacople: consumidor apagado, orden `APROBADA`, lag mayor que 0 y, al volver a encenderlo, lag 0 y evento consumido.

**Importante:** según la guía de S08, cada captura del informe debe mostrar sin recortar el reloj del sistema y el usuario/perfil visible.
