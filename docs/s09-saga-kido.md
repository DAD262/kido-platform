# S09 — Saga coreografiada en KIDO

## Objetivo

La compra de un curso deja de depender de una llamada síncrona de Pago a Inscripción. El pago aprobado publica `pago.aprobado`; Inscripción crea el acceso de forma idempotente y publica `inscripcion.activada`. Si no puede crear el acceso, publica `inscripcion.fallida`; Pago compensa la venta y deja la orden `REEMBOLSADA`.

## Flujo

```text
kido-pago-ms
  APROBADA
     |
     | pago.aprobado
     v
Kafka: kido-pago-eventos
     |
     v
kido-inscripcion-ms
  ¿ya existe COMPRA estudiante+curso?
     | sí -> ignored + inscripcion.activada
     | no -> crear ACTIVA + inscripcion.activada
     | error -> inscripcion.fallida
     v
Kafka: kido-academico-eventos
     |
     v
kido-pago-ms
  inscripcion.activada -> inscripcionSincronizada=true
  inscripcion.fallida  -> reversa saldo + reembolso/compensación -> REEMBOLSADA
```

No hay orquestador central. Cada servicio reacciona a eventos: es una **Saga coreografiada**.

## Idempotencia

La inscripción tiene dos capas:

1. revisión explícita por `(estudiante_id, curso_id)` antes de crear;
2. restricción `UNIQUE(estudiante_id, curso_id)` en PostgreSQL como garantía ante carreras.

Al repetir `pago.aprobado`, no se crea una segunda inscripción. Se registra `status=ignored` y se vuelve a publicar `inscripcion.activada`, permitiendo que Pago converja a `inscripcionSincronizada=true` aunque el ACK anterior se hubiera perdido.

La compensación también es idempotente. Pago solo compensa una orden `APROBADA`; si ya está `REEMBOLSADA`, un `inscripcion.fallida` repetido produce `status=ignored`.

## Compensación

Cuando Inscripción publica `inscripcion.fallida`:

- una venta simulada se marca compensada sin llamar a un proveedor externo;
- una venta real intenta el reembolso de Mercado Pago con `X-Idempotency-Key=KIDO-SAGA-{ordenId}`;
- el movimiento del docente pasa a `REVERTIDO`;
- la orden pasa de `APROBADA` a `REEMBOLSADA`.

Esto conserva trazabilidad: la orden no se borra.

## Fallo reproducible para la evidencia

Solo en perfil DEV, `estudianteId=999999` fuerza un fallo de inscripción. En PROD el valor es `0`, por lo que el gatillo queda deshabilitado. Es equivalente al fallo determinista usado en la guía del curso, pero aplicado al dominio KIDO.

## Prueba automática

Con el stack levantado:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-s9-saga.ps1
```

El script demuestra:

1. compra normal e inscripción activada;
2. `pago.aprobado` duplicado ignorado;
3. fallo de inscripción y compensación de la compra;
4. compensación repetida ignorada;
5. consumer groups y lag.

## Estado de la entidad OrdenCompra

```text
PENDIENTE
   |
   v
PAGO_INICIADO
   |
   v
APROBADA ----------------------> REEMBOLSADA
   |                                ^
   | inscripcion.activada           | inscripcion.fallida
   v                                |
APROBADA + inscripcion sincronizada-+
```

`RECHAZADA` sigue reservado para un pago que la pasarela no aprueba antes de completar la venta. `REEMBOLSADA` representa una venta que sí fue aprobada pero debió compensarse después.

## Limitación conocida y siguiente paso de producción

`pago.aprobado` se publica después del commit. Existe una ventana pequeña entre guardar la orden y publicar el evento. Para producción real, el siguiente endurecimiento debe ser **Transactional Outbox**, de modo que estado y evento se persistan en la misma transacción local y un publicador confiable entregue después a Kafka.
