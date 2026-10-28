import { Component, computed, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Api, mensajeError } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { Curso } from '../../core/models/api.models';
import { CursoCard } from '../../shared/curso-card/curso-card';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, FormsModule, CursoCard, Icon],
  templateUrl: './home.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './home.scss',
})
export class Home implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  readonly auth = inject(Auth);

  readonly cursos = signal<Curso[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  busqueda = '';

  readonly recientes = computed(() => [...this.cursos()].sort((a, b) => b.id - a.id).slice(0, 6));
  readonly categorias = computed(() =>
    [
      ...new Set(
        this.cursos()
          .map((c) => c.categoria)
          .filter(Boolean),
      ),
    ].slice(0, 6),
  );
  readonly gratuitos = computed(() => this.cursos().filter((c) => c.tipo === 'GRATUITO').length);

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.api.publicados().subscribe({
      next: (c) => {
        this.cursos.set(c);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeError(e));
        this.cargando.set(false);
      },
    });
  }

  buscar() {
    const q = this.busqueda.trim();
    void this.router.navigate(['/cursos'], { queryParams: q ? { q } : {} });
  }
}
