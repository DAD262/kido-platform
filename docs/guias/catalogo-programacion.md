# Ampliación local de programación

El catálogo local contiene 12 cursos publicados: se conservaron los cuatro anteriores y se agregaron ocho. HTML/CSS y Git son gratuitos; JavaScript cuesta S/ 39.90, TypeScript S/ 49.90, Angular S/ 89.90, SQL S/ 59.90, automatización con Python S/ 69.90 y Java orientado a objetos S/ 74.90. Los importes de los cursos de pago existentes no cambiaron. Los nuevos cursos utilizan al docente ya asociado al catálogo local; su nombre se consulta desde los perfiles verificados.

Cada ruta incorpora tres módulos con dos lecciones por módulo: 72 guías originales en total, con explicación, ejemplo, ejercicio, criterios de comprobación y documentación oficial. Son lecturas y prácticas, no grabaciones de video. Se pueden imprimir o guardar como PDF desde el navegador. Los enlaces externos son referencias; no se copian manuales completos.

Los cuatro cursos archivados de prueba fueron retirados de los listados mediante `retirado_catalogo`; sus cursos, lecciones, compras e inscripciones se conservan. El endpoint de consulta por ID sigue disponible para los accesos históricos. Eliminar un curso no publicado ahora lo retira lógicamente y nunca realiza borrado en cascada.

Las lecciones anteriores conservan sus IDs. Los enlaces vacíos o ficticios se sustituyeron por guías válidas. Las inscripciones anteriores mantienen su lista de progreso, certificados y condiciones: las lecciones añadidas se muestran como complementarias para ellas. Una inscripción nueva utiliza el temario ampliado.

Para reproducir el contenido, `node scripts/catalogo-programacion-2026.cjs` genera HTML y un SQL transaccional en `tmp/catalogo-2026/aplicar.sql`. No se aplica automáticamente a ninguna base. `node scripts/verificar-guias-programacion.cjs` comprueba integridad de las guías y enlaces internos.

La aplicación local usa una copia previa de cursos y huellas de compras e inscripciones en `tmp/catalogo-2026`. No se ejecutaron pagos ni se modificaron operaciones financieras para ampliar el catálogo.
