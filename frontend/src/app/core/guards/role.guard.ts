import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth, Rol } from '../services/auth';

export const roleGuard = (rol: Rol): CanActivateFn => {
  return async (_route, state) => {
    const auth = inject(Auth);
    const router = inject(Router);

    if (!auth.disponible) {
      return router.createUrlTree(['/login'], { queryParams: { error: 'identidad' } });
    }
    if (!auth.autenticado) {
      await auth.login(state.url);
      return false;
    }
    if (auth.tieneRol(rol)) {
      return true;
    }
    // Antes redirigía al inicio sin explicación; ahora se informa por qué.
    return router.createUrlTree(['/login'], { queryParams: { error: 'rol', requerido: rol } });
  };
};

/** Solo exige sesión iniciada (cualquier rol). */
export const sesionGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);
  if (!auth.disponible) {
    return router.createUrlTree(['/login'], { queryParams: { error: 'identidad' } });
  }
  if (!auth.autenticado) {
    await auth.login(state.url);
    return false;
  }
  return true;
};
