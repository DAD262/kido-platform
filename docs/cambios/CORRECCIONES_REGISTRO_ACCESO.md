# Registro y acceso: validacion de credenciales

27 de octubre de 2026.

Se verifico que la cuenta reportada existe, esta habilitada y dispone de una credencial de contrasena. No tiene acciones obligatorias pendientes. El rechazo observado es invalid_user_credentials.

Se probo el registro mediante API y mediante navegador, seguido de login con la misma clave y carga del perfil. Ambos flujos funcionan. Las cuentas temporales de prueba se eliminaron de Keycloak y de perfiles.

Cambios preventivos:

- Confirmacion obligatoria de contrasena en el registro de estudiantes y docentes.
- Avisos de Bloq Mayus y espacios en los formularios de registro y acceso.
- Desactivacion de autocapitalizacion en los campos de clave.
- Se retiro maxlength=72 del HTML para impedir el recorte silencioso de claves pegadas. La validacion existente rechaza explicitamente mas de 72 caracteres.
- Herramienta scripts/comprobar-clave-local.ps1: prueba una clave introducida mediante Read-Host -AsSecureString, sin restablecerla ni guardar el valor o los tokens. El resultado contiene solo estado y motivo.

La prueba local de la cuenta reportada devolvio Invalid user credentials. Se conservo la contrasena a peticion del usuario. No se restablecio ninguna credencial existente.

Validacion: compilacion de produccion correcta y prueba en navegador de confirmacion incorrecta, registro correcto, login y perfil de una cuenta nueva.
