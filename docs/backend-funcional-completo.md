# KIDO — Backend funcional listo para frontend

Esta ampliación completa la lógica de negocio pendiente sin iniciar frontend.

## Curso y contenido
- CRUD existente de cursos.
- Listado de cursos publicados y por docente.
- Módulos y lecciones administrables.
- Materiales por lección: VIDEO, PDF, DOCUMENTO, ENLACE, IMAGEN, AUDIO u OTRO.
- Subida real de archivos multipart, almacenamiento persistente compartido por las instancias de Curso y descarga desde API.
- Configuración por curso de progreso mínimo, asistencia mínima, certificado habilitado y costo del certificado.
- El resumen interno de compra transmite estas reglas hacia Pago e Inscripción.

## Inscripción y aprendizaje
- Inscripción gratuita y por compra.
- Progreso por lección.
- Consulta por estudiante y por curso para construir paneles futuros.
- Asistencia por fecha: PRESENTE, TARDE, FALTA o JUSTIFICADA.
- Cálculo de porcentaje de asistencia y progreso.
- Estado académico que decide si el alumno cumple los requisitos del curso.
- La inscripción se completa cuando cumple progreso y asistencia mínimos.

## Certificados
- Solo se pueden solicitar si el curso los habilita y el estudiante cumple requisitos.
- Certificado gratuito: queda DISPONIBLE inmediatamente.
- Certificado con costo: queda PENDIENTE_PAGO hasta confirmación.
- Código único KIDO-CERT y fecha de emisión.
- Generación y descarga de un PDF de certificado desde backend.
- Si una inscripción comprada es revocada por reembolso, su certificado se anula.

## Pago
- La compra de un curso conserva la regla 10 % Kido / 90 % docente.
- Pago sigue creando la inscripción después de aprobación.
- Ahora propaga las reglas académicas y de certificado del curso a la inscripción, de modo que el frontend no inventa reglas.

## Endpoints principales nuevos
- `GET /api/v1/cursos/publicados`
- `GET /api/v1/cursos/docente/{docenteId}`
- `GET /api/v1/cursos/{cursoId}/contenido`
- `POST /api/v1/cursos/{cursoId}/modulos`
- `POST /api/v1/modulos/{moduloId}/lecciones`
- `POST /api/v1/lecciones/{leccionId}/materiales`
- `GET|PUT /api/v1/cursos/{cursoId}/configuracion`
- `GET /api/v1/inscripciones/estudiante/{id}`
- `GET /api/v1/inscripciones/curso/{id}`
- `POST|GET /api/v1/inscripciones/{id}/asistencias`
- `GET /api/v1/inscripciones/{id}/estado-academico`
- `POST|GET /api/v1/inscripciones/{id}/certificado`

## Regla de diseño
El frontend futuro debe consumir estas APIs. La lógica de negocio permanece en backend: precios, acceso, progreso, asistencia, elegibilidad, certificados, comisiones, pagos y estados no se calculan en el navegador.

## Eventos académicos Kafka
- `curso.completado`: genera notificación al estudiante cuando cumple progreso y asistencia.
- `certificado.disponible`: genera notificación cuando el certificado queda habilitado.
- Se conserva `pago.aprobado` de S08.

Esto mantiene Pago e Inscripción desacoplados de Notificación para los eventos principales.
