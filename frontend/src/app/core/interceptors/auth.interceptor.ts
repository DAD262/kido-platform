import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap } from 'rxjs';
import { keycloak } from '../config/keycloak.config';

const API_PREFIX = '/api/v1/';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(API_PREFIX) || !keycloak.authenticated) {
    return next(req);
  }

  // updateToken rechaza la promesa si el refresh token caducó: en ese caso
  // se vuelve a pedir inicio de sesión en lugar de dejar la petición colgada.
  const renovar = keycloak.updateToken(30).catch(async () => {
    await keycloak.login({ redirectUri: window.location.href });
    return false;
  });

  return from(renovar).pipe(
    switchMap(() => {
      const token = keycloak.token;
      return next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req);
    }),
  );
};
