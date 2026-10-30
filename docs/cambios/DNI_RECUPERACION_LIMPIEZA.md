# DNI, recuperacion y limpieza de cuentas

Entrega local del 27 de octubre de 2026.

## Cuentas conservadas

Se conservaron las dos cuentas personales (usuarios adanyana@gmail.com y adanyanar@gmail.com), admin, docente y estudiante. Se eliminaron estudiante-fallo y los perfiles historicos 2001 y 1002, junto con la solicitud docente de demostracion asociada al perfil 1002. No se eliminaron cursos, inscripciones ni transacciones.

El respaldo previo esta en .backups/cuentas-eliminadas-1793125984125.json. Se retiro la cuenta postulante de la importacion inicial de Keycloak y de las opciones de demo del ingreso.

## DNI

- El registro nuevo, de estudiante o docente, exige un DNI de ocho digitos y evita DNI duplicados.
- El DNI profesional debe coincidir con el de la cuenta.
- Las cuentas antiguas muestran DNI pendiente. Administracion puede completar su DNI real en Editar datos; no se inventaron documentos para las cuentas conservadas.
- El DNI se muestra al titular y a ADMIN, no en el seguimiento del supervisor.
- Se valida formato y unicidad; no es una consulta a RENIEC ni una verificacion oficial de identidad.

## Recuperacion

Pantalla: http://localhost:4300/recuperar. Accesible desde el ingreso de KIDO y desde Olvidaste tu contrasena en Keycloak. Administracion tambien puede solicitar el codigo desde Restablecer clave > Enviar codigo al correo.

Todos los flujos publicos verifican primero la posesion del correo. Conocer el correo y el DNI por si solos no autoriza a solicitar una sesion:

1. Se solicita un codigo para el correo.
2. Se confirma el codigo de seis digitos, con vigencia de diez minutos, hasta cinco intentos y uso unico.
3. Para una cuenta exclusivamente ESTUDIANTE, Keycloak recibe su DNI como credencial temporal (temporary=true) y exige UPDATE_PASSWORD al ingresar. Si falta el DNI, el cambio se rechaza hasta que administracion lo complete.
4. Para DOCENTE, ADMIN o SUPERVISOR, aunque tambien tenga ESTUDIANTE, se exige una nueva clave de 8-72 caracteres con mayuscula, minuscula y numero. El cargo lo decide el servidor a partir de los roles actuales de Keycloak.
5. La persona vuelve a ingresar. En el caso del estudiante, debe completar la pantalla de cambio de clave.

Los codigos se almacenan como HMAC-SHA256 vinculados a su solicitud. No aparecen en la respuesta publica ni en los logs. El secreto esta en .env. Se usan respuestas genericas para cuentas inexistentes o deshabilitadas y espera de sesenta segundos entre envios al mismo correo.

La recuperacion con DNI temporal fue probada contra la politica activa de esta instalacion. No se redujo ni modifico globalmente la politica de Keycloak. Un realm nuevo con una politica que prohiba claves numericas requiere una politica compatible o sustituir el DNI por una clave temporal aleatoria; el archivo de importacion conserva su politica fuerte. Las sesiones JWT emitidas previamente conservan los limites de caducidad ya documentados.

## Correo local y SMTP real

Bandeja de pruebas: http://localhost:18025 (Mailpit). Los correos quedan en esta computadora y NO se entregan a Gmail u otros buzones reales. La aplicacion indica este modo de pruebas.

Mailpit esta publicado solo en 127.0.0.1. SMTP solo se expone en la red interna de Docker. Se usa su [imagen oficial](https://mailpit.axllent.org/docs/install/docker/).

Para correo real, configurar en .env RECUPERACION_SMTP_HOST, PORT, USER, PASS, AUTH, TLS y FROM, y KIDO_APP_URL. Recrear kido-usuario-ms. Nunca compartir la clave SMTP ni KIDO_RECUPERACION_SECRET en el chat o en control de versiones.

## Colores

Administrador azul, supervisor violeta, docente ambar y estudiante verde. Las etiquetas conservan el texto del cargo y funcionan en temas claro y oscuro.

## Pruebas completadas

- 29 pruebas Maven del servicio de usuarios, incluyendo arranque del contexto de recuperacion.
- 9 pruebas del frontend y compilacion de produccion.
- Registro rechazado sin DNI y por DNI duplicado.
- Correo entregado a Mailpit y espera entre reenvios.
- DNI temporal estudiantil con cambio obligatorio.
- Recuperacion real de fixtures DOCENTE, ADMIN y SUPERVISOR con nueva clave; la anterior se rechazo.
- Codigos incorrectos, caducados, usados y bloqueados por cinco intentos rechazados.
- Cuenta inexistente devuelve respuesta generica sin revelar codigos.
- En navegador, recuperacion estudiantil y docente, cambio obligatorio al entrar con DNI, acceso posterior con clave nueva y rechazo del DNI anterior.
- Revision movil del registro, recuperacion y colores de categorias.

Todas las cuentas y desafios temporales de prueba se eliminaron. Los mensajes de prueba de destinatarios example.invalid quedan en la bandeja para revisar su formato; no corresponden a cuentas activas.

Dockerfile del servicio: usa cache de Maven y etapa verify para ejecutar pruebas, evitando el complemento go-offline que fallo al descargar dependencias en esta sesion.
