# Lista de microservicios de Kido

## Microservicios de negocio

| Microservicio | Responsabilidad | Base de datos | Responsable | Estado |
|---|---|---|---|---|
| `kido-curso-ms` | Cursos gratuitos/pagados, categorías, módulos y lecciones | `kido_curso_db` | Adan | Implementado |
| `kido-inscripcion-ms` | Acceso del estudiante y progreso por lección | `kido_inscripcion_db` | Adan | Implementado |
| `kido-pago-ms` | Órdenes, Mercado Pago sandbox, comisión, saldo docente, retiros y reembolsos | `kido_pago_db` | Midwar | Implementado |
| `kido-notificacion-ms` | Bandeja de avisos y canal de correo configurable | `kido_notificacion_db` | Midwar | Implementado |

## Infraestructura compartida

| Componente | Función | Estado |
|---|---|---|
| `kido-config` | Configuración DEV/PROD centralizada | Implementado |
| `kido-eureka` | Registro y descubrimiento | Implementado |
| `kido-gateway` | Punto único de acceso con rutas `lb://` | Implementado |

## Reglas de negocio ya programadas

- Curso `GRATUITO`: no crea orden de pago.
- Curso `PAGO`: crea una orden con snapshot del precio y docente.
- Comisión de Kido: 10 %.
- Saldo del docente: 90 % de la venta, pendiente durante 7 días.
- Retiro: mínimo S/ 50 mediante cuenta bancaria, Yape o Plin.
- Estados del retiro: `PENDING`, `APPROVED`, `PAID`, `REJECTED`.
- Reembolso: hasta 7 días y con progreso máximo de 20 %.
- Un pago aprobado crea una inscripción de tipo `COMPRA` de manera idempotente.
- Un reembolso aprobado revoca la inscripción y revierte el movimiento de la venta.
- Mercado Pago: integración HTTP real preparada para credenciales `TEST-...`; en el entorno local existe simulación explícita para la demo sin versionar secretos.

## Trabajo compartido de una unidad posterior

`kido-auth-ms`, JWT/roles, Kafka, Resilience4j, observabilidad completa y Angular se mantienen como integración compartida de las unidades posteriores. No son necesarios para cerrar la evaluación S5 de Unidad I.
