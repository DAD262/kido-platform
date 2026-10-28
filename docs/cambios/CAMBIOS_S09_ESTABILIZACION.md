# KIDO — S09: estabilidad, Saga y pruebas

## Cambios aplicados

- Compatibilidad de migración S08 → S09: los nuevos eventos usan `schemaVersion=2`; el nuevo grupo consumidor no compensa órdenes históricas S08 sin esa marca.
- Consumidor idempotente en Inscripción: verificación explícita y restricciones únicas de PostgreSQL, incluidas las compras repetidas y las condiciones de carrera.
- **Compensación solo ante errores definitivos del negocio**. Un fallo de conexión con DB/Kafka reintenta el mensaje sin reembolsar automáticamente.
- Publicación confirmada del resultado de la Saga antes de completar el procesamiento del mensaje; si Kafka falla, el evento se reprocesa.
- Validación de orden, estudiante y curso en los resultados de la Saga; fallo tardío ignorado si la inscripción ya fue confirmada.
- El snapshot de lecciones y requisitos de certificado se persiste con la orden ANTES del pago; aprobar/republicar ya no depende de una consulta HTTP a Curso.
- Reintento de inscripción ahora republica el evento por Kafka; ya no utiliza una llamada síncrona a Inscripción.
- Prueba reproducible del fallo con variable explícita `KIDO_SAGA_ESTUDIANTE_FALLO_DEMO=999999`, desactivada por defecto.
- Evita que la repetición del evento cree notificaciones duplicadas mediante `evento_clave` y UNIQUE de PostgreSQL.
- Idempotencia al aprobar un certificado dos veces; acceso a rutas simuladas de pago limitado al rol ADMIN.
- Tests de Saga para inscripción, fallo transitorio, rechazo, duplicados y compensación; scripts de compilación/verificación y demo con aserciones.
- Documentación de la secuencia, diagrama de estados, evidencia académica y límites para despliegue real.

## Cómo empezar

`docs/s09-implementacion-validacion.md` presenta paso a paso la preparación, el script y las pruebas.

## Advertencia sobre producción

Esta entrega es un **candidato S09 para validación local**; necesita compilación Maven y pruebas Docker en el equipo de desarrollo. No se ha afirmado despliegue público ni se deben activar pagos reales hasta completar los pendientes críticos del documento S09 y `docs/roadmap-produccion.md`.
