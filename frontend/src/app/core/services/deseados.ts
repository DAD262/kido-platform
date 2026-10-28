import { Injectable, inject, signal } from '@angular/core';
import { finalize, Observable } from 'rxjs';
import { Api, mensajeError } from './api';
import { Auth } from './auth';

/** Estado compartido: persistencia exclusivamente en el backend; sin localStorage. */
@Injectable({ providedIn: 'root' })
export class Deseados {
  private readonly api = inject(Api);
  readonly auth = inject(Auth);
  readonly ids = signal<ReadonlySet<number>>(new Set<number>());
  readonly cargando = signal(false);
  readonly pendiente = signal<ReadonlySet<number>>(new Set<number>());
  readonly error = signal('');
  private cargadoPara = '';

  get permitido(): boolean {
    return this.auth.autenticado && this.auth.tieneRol('ESTUDIANTE');
  }

  cargar(force = false): void {
    if (!this.permitido) {
      this.ids.set(new Set<number>());
      this.cargadoPara = '';
      return;
    }
    const sub = this.auth.subject;
    if (this.cargando() || (!force && this.cargadoPara === sub)) return;
    this.cargando.set(true);
    this.api.deseados().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: (rows) => {
        this.ids.set(new Set(rows.map((x) => x.cursoId)));
        this.cargadoPara = sub;
        this.error.set('');
      },
      error: (e) => {
        this.cargadoPara = '';
        this.error.set(mensajeError(e));
      },
    });
  }

  contiene(id: number): boolean {
    return this.ids().has(id);
  }

  alternar(id: number): void {
    if (!this.permitido) {
      void this.auth.login('/cursos/' + id);
      return;
    }
    if (this.pendiente().has(id)) return;
    this.error.set('');
    this.pendiente.update((s) => new Set([...s, id]));
    const quitar = this.contiene(id);
    const op: Observable<unknown> = quitar ? this.api.quitarDeseado(id) : this.api.guardarDeseado(id);
    op.pipe(finalize(() => {
      this.pendiente.update((s) => { const n = new Set(s); n.delete(id); return n; });
    })).subscribe({
      next: () => {
        this.ids.update((s) => {
          const n = new Set(s);
          if (quitar) n.delete(id);
          else n.add(id);
          return n;
        });
      },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }
}
