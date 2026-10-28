# Gestion de cuentas y supervision academica

Implementado el 27 de octubre de 2026.

## Administracion

En Administracion > Usuarios y docentes:

- Listado de personas con busqueda por nombre, correo o usuario, categorias y paginacion.
- Categorias: administradores, supervisores, docentes, estudiantes y sin rol. Una persona con varios roles aparece en su categoria principal: ADMIN > SUPERVISOR > DOCENTE > ESTUDIANTE.
- Edicion de nombres, apellidos, correo y habilitacion. El usuario de acceso y los ids de cursos e inscripciones se conservan.
- Restablecimiento de contrasena con confirmacion, opcion de mostrar ambas entradas y cambio obligatorio en el siguiente ingreso activado por defecto.
- No se pueden leer claves existentes. Los valores de contrasena no se incluyen en respuestas, archivos de resultados ni registros de auditoria.
- No se pueden desactivar administradores desde este panel ni modificar cuentas tecnicas de servicio.
- La edicion sincroniza Keycloak y el perfil local. Si falla el guardado del perfil se intenta restaurar la representacion anterior en Keycloak.
- Auditoria en logs: identificador del administrador, identificador de la cuenta y operacion, sin claves.

El registro publico y la verificacion docente mantienen sus reglas: el administrador no otorga DOCENTE saltando la revision de la postulacion. El panel no incluye un mecanismo para promover cuentas a ADMIN.

## Supervisor

Rol SUPERVISOR, con panel /supervisor.

El administrador abre Supervision en la fila de una cuenta, habilita el rol y selecciona los cursos permitidos. La persona debe renovar su sesion para recoger el rol nuevo. Puede supervisar varios cursos; sin asignaciones el panel muestra un estado vacio.

El supervisor consulta exclusivamente:

- Sus cursos asignados y el docente de cada curso.
- Los estudiantes inscritos, estado y porcentaje de progreso.
- Modulos, lecciones y enlaces de las clases virtuales de esos cursos.

No administra usuarios, contrasenas, verificaciones docentes ni finanzas de administracion. La restriccion de cursos se comprueba en el servidor antes de consultar los otros servicios. No se muestran perfiles de administradores ni directorios ajenos a las clases asignadas.

Las asignaciones persisten en supervision_cursos (migracion V5). La configuracion inicial de Keycloak incluye SUPERVISOR. Para un realm preexistente, sincronizar el rol con scripts/sincronizar-keycloak-local.ps1 usando credenciales administrativas vigentes; en este entorno se completo la sincronizacion y se elimino la cuenta administrativa temporal.

## Endpoints

Solo ADMIN:

- GET /api/v1/usuarios/admin/cuentas
- PUT /api/v1/usuarios/admin/cuentas/{id}
- POST /api/v1/usuarios/admin/cuentas/{id}/password
- GET /api/v1/usuarios/admin/cuentas/{id}/supervision
- PUT /api/v1/usuarios/admin/cuentas/{id}/supervision

SUPERVISOR (o ADMIN, limitado a sus propias asignaciones):

- GET /api/v1/usuarios/supervision/cursos
- GET /api/v1/usuarios/supervision/cursos/{id}

## Validacion

- Suite Maven completa del servicio de usuarios, incluyendo 7 pruebas nuevas de protecciones y aislamiento.
- Compilacion de produccion del frontend.
- Integracion real con cuenta temporal: listado, edicion y sincronizacion de perfil, clave invalida rechazada, restablecimiento, clave vieja rechazada y nueva aceptada, desactivacion, asignacion y retiro de supervision.
- Respuestas 401 sin sesion, 403 para estudiante/supervisor en administracion y 404 para clases no asignadas.
- Prueba de navegador: edicion, restablecimiento y asignaciones desde el panel, ingreso del supervisor y seguimiento; revision visual en escritorio y movil.
- Todas las cuentas y asignaciones creadas para las pruebas se eliminaron al terminar. No se cambiaron claves de cuentas existentes durante estas pruebas.

## Limites operativos

Los JWT emitidos antes de un cambio de rol pueden seguir vigentes hasta caducar; volver a iniciar sesion recoge los cambios. Desactivar una cuenta impide nuevos ingresos. El listado carga las cuentas y sus roles desde Keycloak y pagina en el cliente, adecuado para este entorno local; para directorios grandes conviene introducir paginacion y busqueda en el servidor.
