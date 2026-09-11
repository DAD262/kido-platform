# API del aporte de Midwar

Todas las rutas públicas se consumen mediante Gateway en `http://localhost:18080`.

## Pagos

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/v1/pagos/ordenes` | Crea orden de un curso pagado |
| `GET` | `/api/v1/pagos/ordenes/{id}` | Consulta una orden |
| `GET` | `/api/v1/pagos/ordenes/docente/{id}` | Ventas de un docente |
| `GET` | `/api/v1/pagos/ordenes/estudiante/{id}` | Compras de un estudiante |
| `POST` | `/api/v1/pagos/ordenes/{id}/mercado-pago` | Crea preferencia real en sandbox |
| `POST` | `/api/v1/pagos/ordenes/{id}/confirmar-mercado-pago?paymentId=...` | Verifica un pago real con Mercado Pago |
| `POST` | `/api/v1/pagos/ordenes/{id}/simular-aprobacion` | Solo demo local; aprueba sin credenciales |
| `POST` | `/api/v1/pagos/ordenes/{id}/reintentar-inscripcion` | Reintenta la sincronización del acceso |
| `GET` | `/api/v1/pagos/docentes/{id}/saldo` | Saldo pendiente/disponible/reservado/retirado |

Crear orden:

```json
{
  "estudianteId": 15,
  "cursoId": 2
}
```

## Retiros

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/v1/retiros` | Solicita un retiro |
| `GET` | `/api/v1/retiros/docente/{id}` | Historial del docente |
| `PUT` | `/api/v1/retiros/{id}/aprobar` | Administrador aprueba |
| `PUT` | `/api/v1/retiros/{id}/pagar` | Registra transferencia realizada |
| `PUT` | `/api/v1/retiros/{id}/rechazar` | Rechaza y libera la reserva |

```json
{
  "docenteId": 1,
  "monto": 50.00,
  "medio": "YAPE",
  "destino": "999999999"
}
```

Para marcarlo como pagado:

```json
{
  "codigoOperacion": "YAPE-OP-001"
}
```

## Reembolsos

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/v1/reembolsos` | Solicitud con validación de plazo y progreso |
| `PUT` | `/api/v1/reembolsos/{id}/aprobar` | Revoca acceso y revierte saldo |
| `PUT` | `/api/v1/reembolsos/{id}/rechazar` | Registra rechazo |

```json
{
  "ordenId": 1,
  "motivo": "El curso no era lo esperado"
}
```

## Notificaciones

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/v1/notificaciones/usuario/{id}` | Bandeja del usuario |
| `GET` | `/api/v1/notificaciones/usuario/{id}/no-leidas` | Solo pendientes de lectura |
| `PUT` | `/api/v1/notificaciones/{id}/leer` | Marca como leída |

Los endpoints `/internal/v1/**` no tienen ruta en Gateway. Se reservan para comunicación entre microservicios por Eureka/LoadBalancer.
