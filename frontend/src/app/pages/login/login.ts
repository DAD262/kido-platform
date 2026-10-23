import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Auth } from '../../core/services/auth';
import { AuthShell } from '../../shared/auth-shell/auth-shell';
import { Icon } from '../../shared/icon/icon';

/** Solo se aceptan rutas internas conocidas como destino tras el login (evita open redirects). */
const DESTINO_SEGURO =
  /^\/(cursos(\/\d+)?|estudiante|docente|admin|configuracion|postular-docente|pago\/resultado(\?[\w=&%.\-]*)?)?$/;

const ROL_LEGIBLE: Record<string, string> = {
  ADMIN: 'administración',
  DOCENTE: 'docentes verificados',
  ESTUDIANTE: 'estudiantes',
};

/** Cuentas sembradas en infra/keycloak/kido-realm.json para la demostración. */
export const CUENTAS_DEMO = [
  {
    usuario: 'estudiante',
    clave: 'Estudiante123!',
    rol: 'Estudiante',
    detalle: 'Cursos, avance y certificados',
  },
  {
    usuario: 'docente',
    clave: 'Docente123!',
    rol: 'Docente',
    detalle: 'Docente verificado con cursos',
  },
  {
    usuario: 'postulante',
    clave: 'Postulante123!',
    rol: 'Postulante',
    detalle: 'Solicitud docente en revisión',
  },
  {
    usuario: 'admin',
    clave: 'Admin123!',
    rol: 'Administración',
    detalle: 'Aprueba cursos, pagos y docentes',
  },
] as const;

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink, Icon, AuthShell],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements OnInit {
  readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly demo = CUENTAS_DEMO;
  readonly aviso = signal('');
  readonly registrado = signal('');
  readonly redirigiendo = signal(false);
  readonly abriendo = signal(false);
  readonly copiado = signal('');

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    const error = q.get('error');
    this.registrado.set(q.get('registrado') ?? '');

    if (error === 'identidad' || !this.auth.disponible) {
      this.aviso.set(
        'El servicio de inicio de sesión (Keycloak) no responde. Comprueba que el contenedor kido-keycloak esté activo e inténtalo de nuevo.',
      );
      return;
    }
    if (error === 'rol') {
      const requerido = q.get('requerido') ?? '';
      const quien = ROL_LEGIBLE[requerido] ?? 'otro perfil';
      this.aviso.set(
        requerido === 'DOCENTE'
          ? 'El panel de docente es para cuentas verificadas. Si quieres enseñar, envía tu postulación y administración la revisará.'
          : `Esa sección es para ${quien}. Tu cuenta actual es «${this.auth.etiquetaRol}».`,
      );
      return;
    }
    if (this.auth.autenticado) {
      this.redirigiendo.set(true);
      const pedido = q.get('returnUrl');
      const destino = pedido && DESTINO_SEGURO.test(pedido) ? pedido : this.auth.obtenerPanel();
      void this.router.navigateByUrl(destino);
    }
  }

  esErrorDocente(): boolean {
    return this.route.snapshot.queryParamMap.get('requerido') === 'DOCENTE';
  }

  iniciarSesion(loginHint?: string): void {
    if (this.abriendo()) return;
    this.abriendo.set(true);
    const pedido = this.route.snapshot.queryParamMap.get('returnUrl') ?? undefined;
    void this.auth
      .login(
        pedido && DESTINO_SEGURO.test(pedido) ? pedido : undefined,
        loginHint || this.registrado() || undefined,
      )
      .finally(() => this.abriendo.set(false));
  }

  async copiar(texto: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(texto);
      this.copiado.set(texto);
      setTimeout(() => this.copiado.set(''), 1600);
    } catch {
      /* Sin permiso de portapapeles: el texto sigue visible. */
    }
  }

  recargar(): void {
    window.location.href = '/login';
  }
}
