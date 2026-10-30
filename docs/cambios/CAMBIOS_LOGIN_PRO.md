# KIDO - Login profesional y registro

Se renovó el acceso de KIDO manteniendo Keycloak como proveedor de identidad.

## Cambios

- Nueva pantalla `/login` con composición tipo producto SaaS moderno y responsive.
- Se eliminó la sensación de pantalla intermedia genérica y se agregó contexto de producto.
- Acciones separadas para **Iniciar sesión** y **Crear una cuenta**.
- Registro habilitado en el realm `kido`.
- Método `register()` añadido al servicio de autenticación Angular.
- Tema visual `kido` para Keycloak, de modo que la pantalla donde se escriben las credenciales conserve el branding de KIDO.
- Keycloak queda en español por defecto con soporte `es/en`.
- El tema de Keycloak se monta mediante `compose.yml`.

## Probar localmente

Después de reemplazar los archivos, recrear Keycloak para que vuelva a importar el realm y tome el tema:

```powershell
docker compose down
docker compose up -d --build
```

Frontend:

```powershell
cd frontend
npm install
npx ng serve --proxy-config proxy.conf.json
```

Abrir `http://localhost:4300/login`.

> Si Keycloak ya conserva el realm en un entorno persistente, seleccionar manualmente el tema `kido` en Realm settings > Themes > Login theme, y habilitar User registration en Realm settings > Login.
