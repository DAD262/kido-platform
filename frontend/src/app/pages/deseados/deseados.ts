import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, mensajeError } from '../../core/services/api';
import { Deseados } from '../../core/services/deseados';
import { Curso } from '../../core/models/api.models';
import { CursoCard } from '../../shared/curso-card/curso-card';

@Component({
  selector: 'app-deseados',
  standalone: true,
  imports: [RouterLink, CursoCard],
  templateUrl: './deseados.html',
  styleUrl: './deseados.scss',
})
export class ListaDeseados implements OnInit {
  readonly deseados = inject(Deseados);
  private readonly api = inject(Api);
  readonly publicados = signal<Curso[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly cursos = computed(() => this.publicados().filter((c) => this.deseados.contiene(c.id)));

  ngOnInit(): void {
    this.deseados.cargar(true);
    this.api.publicados().subscribe({
      next: (r) => { this.publicados.set(r); this.cargando.set(false); },
      error: (e) => { this.error.set(mensajeError(e)); this.cargando.set(false); },
    });
  }
}
