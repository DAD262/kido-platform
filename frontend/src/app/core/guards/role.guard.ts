
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/auth';

export const roleGuard = (rol: string): CanActivateFn => {
  return async () => {
    const auth = inject(Auth);
    const router = inject(Router);

    if (!auth.autenticado) {
      await auth.login();
      return false;
    }

    if (auth.tieneRol(rol)) {
      return true;
    }

    return router.createUrlTree(['/']);
  };
};
