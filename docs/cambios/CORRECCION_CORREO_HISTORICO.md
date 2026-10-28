# Correccion de correo reservado por perfiles historicos

27 de octubre de 2026.

El correo reportado pertenecia al perfil local 2001, cuya identidad Keycloak ya no existia. El listado anterior solo incluia identidades activas, por lo que el perfil que reservaba el correo no era visible.

Cambios:

- El listado administrativo incluye perfiles locales sin identidad en la categoria Perfiles historicos, con estado Sin acceso e identificador de perfil.
- Estos registros no ofrecen edicion de credenciales ni supervision: no tienen una cuenta de acceso activa.
- Al editar el correo de una cuenta activa, una reserva historica se libera solo si la consulta de su antiguo id en Keycloak devuelve 404. Un error de red, permisos o disponibilidad no permite liberar el correo.
- Si la identidad anterior existe o el perfil esta pendiente de vinculacion, el correo sigue protegido.
- La liberacion y actualizacion del perfil destino se hacen en la misma transaccion. Se conserva el perfil historico, su id, username y referencias; solo su campo email queda libre. No se fusionan cuentas ni se transfieren cursos.
- La lista se refresca tras guardar para reflejar el correo actualizado en todas las filas.

Validacion: suite Maven completa, tres pruebas nuevas de reservas activas/errores/identidad inexistente y prueba real con dos cuentas temporales. Se verifico rechazo del correo activo, reutilizacion tras eliminar la identidad de prueba, conservacion del perfil e ingreso por el correo actualizado. Las cuentas temporales se eliminaron y ninguna cuenta existente se modifico durante las pruebas.
