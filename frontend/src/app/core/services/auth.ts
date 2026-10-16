
import { Injectable } from '@angular/core';
import { keycloak } from '../config/keycloak.config';

@Injectable({
  providedIn: 'root'
})
export class Auth {

  get autenticado(): boolean {
    return keycloak.authenticated ?? false;
  }

  get subject(): string {
    return keycloak.subject ?? 'guest';
  }

  get usuario(): string {
    return keycloak.tokenParsed?.['preferred_username'] ?? '';
  }

  get roles(): string[] {
    return keycloak.realmAccess?.roles ?? [];
  }

  tieneRol(rol: string): boolean {
    return this.roles.includes(rol);
  }

  obtenerPanel(): string {
    if (this.tieneRol('ADMIN')) return '/admin';
    if (this.tieneRol('DOCENTE')) return '/docente';
    if (this.tieneRol('ESTUDIANTE')) return '/estudiante';

    return '/';
  }

  async login(): Promise<void> {
    await keycloak.login({
      redirectUri: window.location.origin + '/login'
    });
  }

  async logout(): Promise<void> {
    await keycloak.logout({
      redirectUri: window.location.origin
    });
  }
}
