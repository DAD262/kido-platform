# Diseño y experiencia: catálogo y aprendizaje

Primera mejora aplicada el 27 de octubre de 2026:

- Catálogo con cabecera nueva, filtros visibles y contador de resultados.
- Búsqueda, categoría, precio y orden conservados en la URL; el orden sobrevive a recargar.
- Controles de filtro con estados accesibles y opción de limpiar filtros.
- Tarjetas con acceso al detalle siempre visible y controles de deseados de 44 px.
- Panel del estudiante con resumen de cursos, avance y cursos completados.
- Filtros Todos / En curso / Completados y estado explícito sin coincidencias.
- Orientación para la primera inscripción y para seleccionar un curso.
- Navegación móvil en dos filas, sin desbordamiento horizontal con sesión iniciada.
- Colores compatibles con temas claro y oscuro existentes.

Validación: compilación de producción correcta; navegador Chrome aislado a 1440 px y 390 px; login real con cuenta demo; filtros y orden al recargar; selección y carga de lecciones. No se modificaron pagos, roles ni datos de aprendizaje.

La compilación mantiene avisos no bloqueantes de imports no usados en Docente/Admin y presupuestos de estilos en Registro y Estudiante. El tamaño inicial permanece por debajo del presupuesto de 500 kB.
