# kido-inscripcion-ms

Microservicio transaccional de Adan. Inscripcion es la cabecera y ProgresoLeccion es el detalle.

## Operaciones

| Método | Ruta |
|---|---|
| GET | `/api/v1/inscripciones` |
| GET | `/api/v1/inscripciones/{id}` |
| POST | `/api/v1/inscripciones` |
| PUT | `/api/v1/inscripciones/{id}/estado` |
| PUT | `/api/v1/inscripciones/{id}/lecciones/{leccionId}/completar` |
| DELETE | `/api/v1/inscripciones/{id}` |

En S5 solo crea inscripciones gratuitas. Las compras se habilitan en Unidad 2 después del pago aprobado.
