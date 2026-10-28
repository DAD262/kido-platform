# Correccion de usuario, correo y portal personal

27 de octubre de 2026.

## Acceso y administracion

- Login admite el usuario de acceso y el correo actual de la misma persona.
- Editar datos incluye usuario, correo, DNI, nombres, apellidos y estado.
- Se verifica que un identificador no pertenezca a otra cuenta ni sea ambiguo entre el usuario de una persona y el correo de otra, incluso si la otra cuenta esta deshabilitada.
- Se impiden prefijos de cuentas tecnicas y se conservan los ids de personas, cursos e inscripciones.
- Username se sincroniza en Keycloak y en perfiles. Una falla de escritura local intenta compensar tambien el usuario anterior en Keycloak.
- Al volver de la cuenta personal, GET /usuarios/me sincroniza nombres, usuario y correo desde la identidad vigente, no desde los datos potencialmente antiguos del JWT. DNI e id de KIDO se conservan.

## Mi cuenta y contrasena

El problema era de autorizacion del portal: las cuentas importadas no recibian los roles personales de account ni los clientes emitian correctamente esos permisos. Se aplico la configuracion autorizada por el usuario:

- loginWithEmailAllowed=true, editUsernameAllowed=true, registrationEmailAsUsername=false.
- manage-account y view-profile para la cuenta propia; sin roles administrativos adicionales.
- Scopes de roles para kido-cli, kido-frontend y account-console.
- Configuracion de importacion y script de sincronizacion actualizados para conservar la correccion.
- La herramienta de sincronizacion ahora normaliza correctamente las listas devueltas por PowerShell.
- Se elimino el cliente administrativo temporal usado para la actualizacion.

La opcion Mi cuenta y contrasena sigue usando el portal de Keycloak y permite cambiar la clave sin que KIDO lea la contrasena actual.

## Validacion

- Suite del servicio de usuarios: 35 pruebas correctas, incluyendo colisiones de identificadores, conservacion del id y sincronizacion del perfil con un token antiguo.
- Compilacion del frontend correcta.
- Prueba con cuenta temporal: acceso por usuario y correo al mismo sub, edicion por API y por formulario, rechazo de duplicados y de los identificadores anteriores, id y DNI conservados.
- Navegador: Mi cuenta y contrasena abre Informacion personal sin error; Seguridad de la cuenta > Inicio de sesion permite cambiar la clave; la nueva funciona por usuario y por correo y la anterior se rechaza.
- La cuenta temporal se elimino al terminar. No se cambiaron claves ni identificadores de las cinco cuentas reales.

Las sesiones ya abiertas pueden conservar datos anteriores hasta renovar su token. Si el portal estaba mostrando el error, cerrar esa pestana y volver a abrir Mi cuenta y contrasena; si persiste una sesion antigua, cerrar sesion e ingresar nuevamente.
