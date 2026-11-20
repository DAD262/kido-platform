import { Injectable, inject, signal } from '@angular/core';
import { Api } from './api';
import { ValoracionCurso } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class Valoraciones {
  private readonly api = inject(Api);
  private iniciado = false;
  readonly datos = signal(new Map<number, ValoracionCurso>());
  cargar(forzar = false) {
    if (this.iniciado && !forzar) return;
    this.iniciado = true;
    this.api.valoracionesCursos().subscribe({
      next: lista => this.datos.set(new Map(lista.map(v => [v.cursoId, v]))),
      error: () => { this.iniciado = false; },
    });
  }
}
