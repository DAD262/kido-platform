import { portadaCurso } from '../../core/utils/curso-portada';
import { Component, computed, inject, input, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Curso } from '../../core/models/api.models';
import { SolesPipe } from '../../core/pipes/estado.pipe';
import { Deseados } from '../../core/services/deseados';

@Component({
  selector: 'app-curso-card',
  standalone: true,
  imports: [RouterLink, SolesPipe],
  templateUrl: './curso-card.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './curso-card.scss',
})
export class CursoCard implements OnInit {
  readonly deseados = inject(Deseados);
  ngOnInit(): void { this.deseados.cargar(); }
  readonly curso = input.required<Curso>();
  readonly portada = computed(() => portadaCurso(this.curso().titulo));
  /** Respaldo para cursos que todavía no tienen una portada temática. */
  readonly inicial = computed(() => (this.curso().titulo?.trim()[0] ?? 'K').toUpperCase());
  readonly tono = computed(() => Math.abs(this.curso().id ?? 0) % 4);
}
