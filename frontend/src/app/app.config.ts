import { ApplicationConfig, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { identidad, keycloak } from './core/config/keycloak.config';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' }),
    ),
    provideAppInitializer(async () => {
      try {
        await keycloak.init({
          onLoad: 'check-sso',
          pkceMethod: 'S256',
          checkLoginIframe: false,
        });
        // Si la sesión expira mientras el usuario navega, se intenta renovar en silencio.
        keycloak.onTokenExpired = () => {
          keycloak.updateToken(30).catch(() => keycloak.clearToken());
        };
      } catch (error) {
        // Antes: si Keycloak estaba caído, la aplicación entera quedaba en blanco.
        identidad.disponible = false;
        console.warn('Keycloak no disponible; KIDO inicia en modo público.', error);
      }
    }),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
