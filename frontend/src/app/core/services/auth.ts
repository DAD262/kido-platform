import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { identidad, keycloak } from '../config/keycloak.config';

export type Rol = 'ADMIN' | 'SUPERVISOR' | 'DOCENTE' | 'ESTUDIANTE';

const ETIQUETA_ROL: Record<Rol, string> = {
  ADMIN: 'Administración',
  SUPERVISOR: 'Supervisor',
  DOCENTE: 'Docente',
  ESTUDIANTE: 'Estudiante',
};

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly router = inject(Router);

  /** false cuando Keycloak no respondió al iniciar la aplicación. */
  get disponible(): boolean {
    return identidad.disponible;
  }

  get autenticado(): boolean {
    return keycloak.authenticated ?? false;
  }

  get subject(): string {
    return keycloak.subject ?? 'guest';
  }

  get usuario(): string {
    return keycloak.tokenParsed?.['preferred_username'] ?? '';
  }

  /** Nombre para saludar: given_name si existe, si no el usuario. */
  get nombre(): string {
    return keycloak.tokenParsed?.['given_name'] ?? this.usuario;
  }

  get iniciales(): string {
    const base = (keycloak.tokenParsed?.['name'] as string | undefined) ?? this.usuario;
    return (
      base
        .split(/[\s._-]+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((p) => p[0]?.toUpperCase())
        .join('') || '?'
    );
  }

  get roles(): string[] {
    return keycloak.realmAccess?.roles ?? [];
  }

  tieneRol(rol: Rol | string): boolean {
    return this.roles.includes(rol);
  }

  get rolPrincipal(): Rol | null {
    if (this.tieneRol('ADMIN')) return 'ADMIN';
    if (this.tieneRol('SUPERVISOR')) return 'SUPERVISOR';
    if (this.tieneRol('DOCENTE')) return 'DOCENTE';
    if (this.tieneRol('ESTUDIANTE')) return 'ESTUDIANTE';
    return null;
  }

  get etiquetaRol(): string {
    const rol = this.rolPrincipal;
    return rol ? ETIQUETA_ROL[rol] : 'Sin rol asignado';
  }

  obtenerPanel(): string {
    switch (this.rolPrincipal) {
      case 'ADMIN':
        return '/admin';
      case 'SUPERVISOR':
        return '/supervisor';
      case 'DOCENTE':
        return '/docente';
      case 'ESTUDIANTE':
        return '/estudiante';
      default:
        return '/';
    }
  }

  get email(): string {
    return keycloak.tokenParsed?.['email'] ?? '';
  }

  /**
   * Abre el formulario de Keycloak (tema «kido»). Vuelve a /login, que decide el panel según el rol.
   * loginHint precarga el usuario (p. ej. el correo recién registrado o una cuenta demo).
   */
  async login(returnUrl?: string, loginHint?: string): Promise<void> {
    const destino = returnUrl ? `/login?returnUrl=${encodeURIComponent(returnUrl)}` : '/login';
    await keycloak.login({
      redirectUri: window.location.origin + destino,
      locale: 'es',
      loginHint,
    });
  }

  /** El registro vive en KIDO (/registro): ahí se elige estudiante o docente. */
  register(tipo?: 'estudiante' | 'docente'): void {
    void this.router.navigate(['/registro'], tipo ? { queryParams: { tipo } } : {});
  }

  /** Pide un token nuevo para recoger roles recién asignados (p. ej. DOCENTE tras la aprobación). */
  async refrescarSesion(): Promise<boolean> {
    try {
      await keycloak.updateToken(-1);
      return true;
    } catch {
      return false;
    }
  }

  async logout(): Promise<void> {
    await keycloak.logout({ redirectUri: window.location.origin });
  }

  /** Abre la gestión de la cuenta (contraseña, datos) en Keycloak. */
  async cuenta(): Promise<void> {
    await keycloak.accountManagement();
  }
}
