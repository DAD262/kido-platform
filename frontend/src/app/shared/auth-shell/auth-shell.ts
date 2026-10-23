import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { VisualPreferences } from '../../core/services/visual-preferences';
import { Icon } from '../icon/icon';
import { Logo } from '../logo/logo';

export type ModoAcceso = 'ingresar' | 'estudiante' | 'docente';

interface Pitch {
  titulo: string;
  texto: string;
  puntos: { icono: string; texto: string }[];
}

const PITCH: Record<ModoAcceso, Pitch> = {
  ingresar: {
    titulo: 'Aprende a tu ritmo. Enseña lo que sabes.',
    texto: 'Una sola cuenta para estudiar, publicar cursos o administrar la plataforma.',
    puntos: [
      { icono: 'play', texto: 'Retoma cada curso en la lección exacta donde lo dejaste.' },
      { icono: 'award', texto: 'Certificados con código verificable al completar.' },
      { icono: 'shield', texto: 'Acceso seguro con Keycloak: KIDO nunca ve tu contraseña.' },
    ],
  },
  estudiante: {
    titulo: 'Tu primer curso está a un minuto.',
    texto: 'Crea tu cuenta gratis y empieza con los cursos abiertos del catálogo.',
    puntos: [
      { icono: 'book', texto: 'Cursos gratuitos y de pago con docentes verificados.' },
      { icono: 'check', texto: 'Tu avance se guarda lección por lección.' },
      { icono: 'wallet', texto: 'Pagos protegidos con Mercado Pago.' },
    ],
  },
  docente: {
    titulo: 'Enseña en KIDO con un perfil verificado.',
    texto: 'Revisamos cada postulación para que los estudiantes confíen en quién les enseña.',
    puntos: [
      { icono: 'user', texto: 'Tu cuenta se crea al instante y ya puedes aprender.' },
      { icono: 'shield', texto: 'Administración valida tu DNI, grado y documento PDF.' },
      { icono: 'grid', texto: 'Con el rol docente activo, publicas cursos y cobras tus ventas.' },
    ],
  },
};

/**
 * Marco común de /login y /registro: panel de marca a la izquierda (oculto en móvil
 * salvo la cabecera) y el formulario a la derecha.
 */
@Component({
  selector: 'app-auth-shell',
  standalone: true,
  imports: [RouterLink, Icon, Logo],
  templateUrl: './auth-shell.html',
  styleUrl: './auth-shell.scss',
})
export class AuthShell {
  readonly modo = input<ModoAcceso>('ingresar');
  readonly visual = inject(VisualPreferences);
  pitch() {
    return PITCH[this.modo()];
  }
}
