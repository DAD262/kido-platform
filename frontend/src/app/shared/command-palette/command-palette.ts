import {
  Component,
  ElementRef,
  HostListener,
  Injectable,
  computed,
  effect,
  inject,
  signal,
  viewChild,
  ChangeDetectionStrategy
} from '@angular/core';
import { Router } from '@angular/router';
import { Api } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { VisualPreferences } from '../../core/services/visual-preferences';
import { Curso } from '../../core/models/api.models';
import { Icon } from '../icon/icon';

@Injectable({ providedIn: 'root' })
export class CommandPaletteService {
  readonly abierto = signal(false);
  abrir() {
    this.abierto.set(true);
  }
  cerrar() {
    this.abierto.set(false);
  }
}

interface Comando {
  id: string;
  grupo: 'Ir a' | 'Cursos' | 'Acciones';
  titulo: string;
  detalle?: string;
  icono: string;
  ejecutar: () => void;
}

/** Normaliza para buscar sin tildes ni mayúsculas. */
const norm = (t: string) => t.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

/**
 * Paleta de comandos (Ctrl/⌘ + K): navegar, buscar cursos y cambiar el tema
 * sin usar el ratón. Respeta los roles de Keycloak para mostrar solo lo permitido.
 */
@Component({
  selector: 'app-command-palette',
  standalone: true,
  imports: [Icon],
  templateUrl: './command-palette.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './command-palette.scss',
})
export class CommandPalette {
  private readonly router = inject(Router);
  private readonly api = inject(Api);
  private readonly auth = inject(Auth);
  private readonly visual = inject(VisualPreferences);
  readonly palette = inject(CommandPaletteService);

  readonly consulta = signal('');
  readonly activo = signal(0);
  private readonly cursos = signal<Curso[]>([]);
  private cursosCargados = false;
  private readonly input = viewChild<ElementRef<HTMLInputElement>>('campo');

  constructor() {
    effect(() => {
      if (!this.palette.abierto()) return;
      this.consulta.set('');
      this.activo.set(0);
      if (!this.cursosCargados) {
        this.cursosCargados = true;
        this.api.publicados().subscribe({
          next: (c) => this.cursos.set(c),
          error: () => (this.cursosCargados = false),
        });
      }
      setTimeout(() => this.input()?.nativeElement.focus());
    });
  }

  private readonly base = computed<Comando[]>(() => {
    const ir = (url: string) => () => void this.router.navigateByUrl(url);
    const lista: Comando[] = [
      { id: 'inicio', grupo: 'Ir a', titulo: 'Inicio', icono: 'grid', ejecutar: ir('/') },
      {
        id: 'cursos',
        grupo: 'Ir a',
        titulo: 'Explorar cursos',
        icono: 'book',
        ejecutar: ir('/cursos'),
      },
    ];
    if (this.auth.tieneRol('ESTUDIANTE'))
      lista.push({
        id: 'est',
        grupo: 'Ir a',
        titulo: 'Mi aprendizaje',
        icono: 'play',
        ejecutar: ir('/estudiante'),
      });
    if (this.auth.tieneRol('DOCENTE'))
      lista.push({
        id: 'doc',
        grupo: 'Ir a',
        titulo: 'Mis cursos como docente',
        icono: 'book',
        ejecutar: ir('/docente'),
      });
    if (this.auth.autenticado && !this.auth.tieneRol('DOCENTE'))
      lista.push({
        id: 'postular',
        grupo: 'Ir a',
        titulo: 'Postular como docente',
        icono: 'award',
        ejecutar: ir('/postular-docente'),
      });
    if (this.auth.tieneRol('SUPERVISOR')) lista.push({id:'supervision',grupo:'Ir a',titulo:'Supervisión de clases',icono:'book',ejecutar:ir('/supervisor')});
    if (this.auth.tieneRol('ADMIN'))
      lista.push({
        id: 'adm',
        grupo: 'Ir a',
        titulo: 'Administración',
        icono: 'shield',
        ejecutar: ir('/admin'),
      });
    lista.push(
      {
        id: 'pref',
        grupo: 'Ir a',
        titulo: 'Preferencias',
        icono: 'settings',
        ejecutar: ir('/configuracion'),
      },
      {
        id: 'tema',
        grupo: 'Acciones',
        titulo: this.visual.oscuro() ? 'Usar tema claro' : 'Usar tema oscuro',
        icono: this.visual.oscuro() ? 'sun' : 'moon',
        ejecutar: () => this.visual.alternarTema(),
      },
    );
    if (this.auth.autenticado) {
      lista.push(
        {
          id: 'cuenta',
          grupo: 'Acciones',
          titulo: 'Gestionar mi cuenta',
          detalle: 'Keycloak',
          icono: 'user',
          ejecutar: () => void this.auth.cuenta(),
        },
        {
          id: 'salir',
          grupo: 'Acciones',
          titulo: 'Cerrar sesión',
          icono: 'logout',
          ejecutar: () => void this.auth.logout(),
        },
      );
    } else if (this.auth.disponible) {
      lista.push(
        {
          id: 'entrar',
          grupo: 'Acciones',
          titulo: 'Iniciar sesión',
          icono: 'user',
          ejecutar: ir('/login'),
        },
        {
          id: 'registro',
          grupo: 'Acciones',
          titulo: 'Crear cuenta',
          detalle: 'Estudiante o docente',
          icono: 'plus',
          ejecutar: ir('/registro'),
        },
      );
    }
    return lista;
  });

  readonly resultados = computed<Comando[]>(() => {
    const q = norm(this.consulta().trim());
    const cursos: Comando[] = this.cursos().map((c) => ({
      id: 'c' + c.id,
      grupo: 'Cursos',
      titulo: c.titulo,
      detalle: c.categoria,
      icono: 'book',
      ejecutar: () => void this.router.navigate(['/cursos', c.id]),
    }));
    if (!q) return [...this.base(), ...cursos.slice(0, 4)];
    const coincide = (c: Comando) => norm(`${c.titulo} ${c.detalle ?? ''}`).includes(q);
    const lista = [...this.base().filter(coincide), ...cursos.filter(coincide).slice(0, 8)];
    // Siempre ofrecer «buscar en el catálogo» con el texto escrito.
    lista.push({
      id: 'buscar',
      grupo: 'Acciones',
      titulo: `Buscar «${this.consulta().trim()}» en el catálogo`,
      icono: 'search',
      ejecutar: () =>
        void this.router.navigate(['/cursos'], { queryParams: { q: this.consulta().trim() } }),
    });
    return lista;
  });

  /** Índices donde empieza cada grupo, para pintar su título. */
  esInicioDeGrupo(i: number) {
    const r = this.resultados();
    return i === 0 || r[i].grupo !== r[i - 1].grupo;
  }

  ejecutar(cmd: Comando | undefined) {
    if (!cmd) return;
    this.palette.cerrar();
    cmd.ejecutar();
  }

  onInput(valor: string) {
    this.consulta.set(valor);
    this.activo.set(0);
  }

  onKeydown(e: KeyboardEvent) {
    const total = this.resultados().length;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      this.activo.update((i) => (i + 1) % Math.max(total, 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      this.activo.update((i) => (i - 1 + total) % Math.max(total, 1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      this.ejecutar(this.resultados()[this.activo()]);
    }
  }

  @HostListener('document:keydown', ['$event'])
  atajos(e: KeyboardEvent) {
    const escribiendo = /^(INPUT|TEXTAREA|SELECT)$/.test((e.target as HTMLElement)?.tagName ?? '');
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      if (this.palette.abierto()) this.palette.cerrar();
      else this.palette.abrir();
    } else if (e.key === '/' && !escribiendo && !this.palette.abierto()) {
      e.preventDefault();
      this.palette.abrir();
    } else if (e.key === 'Escape' && this.palette.abierto()) {
      this.palette.cerrar();
    }
  }
}
