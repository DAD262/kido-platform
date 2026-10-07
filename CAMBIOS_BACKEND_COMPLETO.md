# KIDO — Cierre funcional de backend antes del frontend

Se completa el backend sobre la base S08 aprobada, manteniendo los cuatro microservicios de negocio y la infraestructura existente.

## Agregado
1. Contenido académico completo: módulos, lecciones y materiales.
2. Reglas configurables por curso: progreso, asistencia y certificado.
3. Consultas necesarias para panel futuro de docente y estudiante.
4. Registro y cálculo de asistencia.
5. Evaluación automática del cumplimiento académico.
6. Certificados gratuitos o con pago pendiente, código único y anulación por reembolso.
7. Propagación de reglas Curso → Pago → Inscripción en compras aprobadas.
8. Migraciones Flyway incrementales; no se reescriben migraciones anteriores.

No se agregó frontend. La intención es que el frontend posterior sea una capa de presentación y no replique reglas de negocio.

9. Eventos académicos Kafka para curso completado y certificado disponible.
10. Reglas de Gateway refinadas por rol para administración de contenido, asistencia y validación de cursos.
