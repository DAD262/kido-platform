import {
  HttpInterceptorFn
} from '@angular/common/http';

import { from, switchMap } from 'rxjs';
import { keycloak } from '../config/keycloak.config';

const API_URL = '/api/v1/';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  if (!(req.url.startsWith(API_URL) || req.url.startsWith('http://localhost:18080/api/v1/')) || !keycloak.authenticated) {
    return next(req);
  }

  return from(keycloak.updateToken(30)).pipe(
    switchMap(() => {
      const token = keycloak.token;

      if (!token) {
        return next(req);
      }

      const securedRequest = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });

      return next(securedRequest);
    })
  );
};