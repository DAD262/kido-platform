import { portadaCurso } from '../../core/utils/curso-portada';
import { Component, computed, inject, input, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { Curso } from '../../core/models/api.models';
import { SolesPipe } from '../../core/pipes/estado.pipe';
import { Deseados } from '../../core/services/deseados';
import { Carrito } from '../../core/services/carrito';
import { Docentes } from '../../core/services/docentes';
import { Valoraciones } from '../../core/services/valoraciones';

@Component({
  selector: 'app-curso-card',
  standalone: true,
  imports: [RouterLink, SolesPipe, DecimalPipe],
  templateUrl: './curso-card.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './curso-card.scss',
})
export class CursoCard implements OnInit {
  readonly docentes=inject(Docentes);
  readonly carrito=inject(Carrito);
  readonly deseados = inject(Deseados);
  readonly valoraciones = inject(Valoraciones);
  readonly estrellas = [1, 2, 3, 4, 5];
  ngOnInit(): void { this.deseados.cargar();this.docentes.cargar();this.carrito.cargar();this.valoraciones.cargar(); }
  readonly curso = input.required<Curso>();
  readonly portada = computed(() => portadaCurso(this.curso().titulo, this.curso().portadaUrl));
  /** Respaldo para cursos que todavía no tienen una portada temática. */
  readonly inicial = computed(() => (this.curso().titulo?.trim()[0] ?? 'K').toUpperCase());
  readonly tono = computed(() => Math.abs(this.curso().id ?? 0) % 4);
  readonly valoracion = computed(() => this.valoraciones.datos().get(this.curso().id));
}
