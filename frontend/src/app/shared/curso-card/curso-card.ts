import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Curso } from '../../core/models/api.models';
import { SolesPipe } from '../../core/pipes/estado.pipe';

@Component({
  selector: 'app-curso-card',
  standalone: true,
  imports: [RouterLink, SolesPipe],
  templateUrl: './curso-card.html',
  styleUrl: './curso-card.scss',
})
export class CursoCard {
  readonly curso = input.required<Curso>();
  /** Portada tipográfica: inicial del título + tono estable según el id. */
  readonly inicial = computed(() => (this.curso().titulo?.trim()[0] ?? 'K').toUpperCase());
  readonly tono = computed(() => Math.abs(this.curso().id ?? 0) % 4);
}
