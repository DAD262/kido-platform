# Portadas y certificados de finalización

Las portadas del catálogo también aparecen en el detalle del curso y en Mi aprendizaje. Se reutiliza el mismo recurso para mantener la presentación consistente.

El certificado se emite automáticamente cuando la inscripción cumple el avance y la asistencia configurados. Cada emisión conserva código y fecha; las descargas usan el nombre verificado del perfil y el título del curso. Los datos de presentación quedan guardados para mantener el documento estable. No se emite para inscripciones canceladas o revocadas. Los certificados con costo adicional requieren confirmación del servicio de pagos.

El alumno encuentra la vista previa, visor ampliado, descarga PDF, descarga PNG, impresión y opciones para compartir dentro de Mi aprendizaje. El PDF es vectorial A4 horizontal, con fuentes incrustadas y caracteres españoles; el PNG se renderiza a 200 DPI. El modelo del detalle está marcado VISTA PREVIA y no es una emisión.

Compartir utiliza el selector de aplicaciones cuando el navegador admite archivos. En otros navegadores descarga el PDF. La opción WhatsApp abre un borrador de texto y explica que el usuario debe adjuntar el archivo antes de enviarlo. La aplicación no envía mensajes automáticamente ni expone un enlace público con datos personales.

Las descargas requieren sesión y propiedad de la inscripción (o rol ADMIN), con Cache-Control private, no-store. La identidad se verifica con el endpoint autenticado existente de usuarios; no se añadieron credenciales ni permisos en Keycloak. El seguimiento de supervisión utiliza el endpoint interno de inscripciones.

Los cursos publicados que tenían el certificado deshabilitado y costo cero pasan a incluirlo. Las inscripciones existentes de los cursos publicados iniciales se benefician sin modificar su avance. El certificado de Python 2.0 conserva su costo adicional configurado de S/ 5; los cursos archivados de prueba no se habilitan.

Verificación realizada:
- 20 pruebas del servicio de inscripciones y certificados, sin fallos.
- 35 pruebas del servicio de usuarios, sin fallos.
- 23 pruebas de interfaz, sin fallos, y compilación de producción correcta.
- Servicios locales actualizados y descargas reales PDF/PNG de Python verificadas.
- Código y fecha estables; acceso ajeno, anónimo, pago manual público y curso incompleto bloqueados.
- Fotos, vista ampliada y adaptación a 390 px y escritorio comprobadas en navegador aislado, sin imprimir ni enviar mensajes.

Evidencias locales: .design-review/certificado-*.png y tmp/pdfs/certificado-python-estudiante.pdf. Estos archivos son evidencia de revisión; la descarga del alumno se realiza desde su sesión en KIDO.
