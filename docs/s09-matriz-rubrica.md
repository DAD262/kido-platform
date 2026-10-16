# S09 — Matriz de revisión y entrega

Revisión estática del ZIP recibido el 14/10/2026. «Presente» significa localizado en código, no validado en ejecución.

| Dimensión | Peso | Estado de KIDO / evidencia pendiente |
|---|---:|---|
| Bug original e idempotencia de PAGATU | 2 | Requiere código/entorno PAGATU y evidencia anterior al fix. El ZIP recibido solo contiene KIDO. No equivale a un bug de KIDO ni a una prueba JUnit. |
| Fallo de pago y compensación de PAGATU | 2 | Pendiente en PAGATU: pago.fallido publicado/consumido y orden CANCELADA. |
| Compensación idempotente de PAGATU | 2 | Pendiente repetir pago.fallido y comprobar ausencia de doble efecto. |
| Proceso propio KIDO | 2 | Presente: Pago → Inscripción → Pago; consulta explícita, UNIQUE y compensación de orden/saldo. Falta ejecución real y capturas. |
| Estados y documentación | 1 | Diagrama corregido según el código; falta incorporar evidencia final. |
| Aporte individual | 1 | El historial recibido identifica aportes previos de ambos integrantes. Registrar el aporte nuevo real con el autor que lo realizó. No atribuir a una persona todo el código del equipo. |
| Orden y reflexión | 1 | Informe base preparado. Faltan capturas, URL GitHub, número de equipo, reflexión revisada y feedback personal. |

Escala por dimensión: Inicio=0, Proceso=1, Logro=2, Destacado=3.
Puntos = suma(peso × nivel); máximo 33. Nota = puntos / 33 × 20.
No se asigna una calificación antes de reunir las evidencias.

## Qué ya existe en KIDO

- Evento `pago.aprobado`, schemaVersion 2, key ordenId.
- Consulta previa al crear la inscripción y UNIQUE por alumno/curso y por orden.
- `inscripcion.activada` y confirmación de sincronización.
- `inscripcion.fallida` ante rechazo permanente; errores transitorios se reintentan.
- Compensación solo si la compra sigue APROBADA y no está sincronizada.
- Estado REEMBOLSADA y movimiento de venta REVERTIDO.
- Guardia contra compensación repetida y fallo tardío.
- Clave única de notificación; pruebas unitarias y demo.

## Cambios de esta actualización

- Filtro de tipos de evento en AcademicoResultadoConsumer, antes de validar ordenId.
- Cuatro pruebas del enrutamiento y una prueba adicional de reversión única del saldo.
- Demo con pausas, logs nuevos por ventana temporal, SELECT reales de PostgreSQL y resumen sin tokens.
- Guía para repetir exactamente el resultado de fallo desde Kafka UI.
- Documentación ajustada: RECHAZADA no tiene transición implementada.

No se añaden dependencias, migraciones, servicios ni cambios de frontend. No se modifica ninguna migración aplicada ni su checksum.

## Axon / Kafka UI

El manual enseña la Saga construida con Kafka y menciona Axon Framework como herramienta alternativa. No exige instalar Axon Server ni migrar la aplicación a Axon. Esta entrega utiliza Kafka UI ya declarada en Compose, puerto 18085. Una integración adicional con Axon requiere identificar el producto y el objetivo; no se presenta como implementada ni se sustituye una pantalla por otra.

## Bono

La Parte E pide stock antes/durante/después en PAGATU (+2). KIDO vende acceso a cursos y no tiene stock de productos. La reversión de saldo satisface la liberación del recurso del proceso propio, pero no se anuncia automáticamente como el bono de stock.
