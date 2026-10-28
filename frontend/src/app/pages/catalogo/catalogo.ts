import { Component, computed, inject, OnInit, signal, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Api, mensajeError } from '../../core/services/api';
import { Deseados } from '../../core/services/deseados';
import { Curso } from '../../core/models/api.models';
import { CursoCard } from '../../shared/curso-card/curso-card';
import { Icon } from '../../shared/icon/icon';

type Tipo = 'TODOS' | 'GRATUITO' | 'PAGO';
type Orden = 'RECIENTES' | 'NOMBRE' | 'PRECIO_MENOR' | 'PRECIO_MAYOR';

const norm = (t: string) => t.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CursoCard, FormsModule, Icon],
  templateUrl: './catalogo.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './catalogo.scss',
})
export class Catalogo implements OnInit {
  private readonly api = inject(Api);
  readonly deseados = inject(Deseados);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly cursos = signal<Curso[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');

  readonly busqueda = signal('');
  readonly tipo = signal<Tipo>('TODOS');
  readonly categoria = signal('TODAS');
  readonly ordenar = signal<Orden>('RECIENTES');

  readonly tipos: { valor: Tipo; texto: string }[] = [
    { valor: 'TODOS', texto: 'Todos' },
    { valor: 'GRATUITO', texto: 'Gratis' },
    { valor: 'PAGO', texto: 'De pago' },
  ];

  readonly categorias = computed(() =>
    [
      ...new Set(
        this.cursos()
          .map((c) => c.categoria)
          .filter(Boolean),
      ),
    ].sort(),
  );

  readonly filtrados = computed(() => {
    const q = norm(this.busqueda().trim());
    const lista = this.cursos().filter(
      (c) =>
        (this.tipo() === 'TODOS' || c.tipo === this.tipo()) &&
        (this.categoria() === 'TODAS' || c.categoria === this.categoria()) &&
        norm(`${c.titulo} ${c.descripcion} ${c.categoria}`).includes(q),
    );
    switch (this.ordenar()) {
      case 'PRECIO_MENOR':
        return [...lista].sort((a, b) => a.precio - b.precio);
      case 'PRECIO_MAYOR':
        return [...lista].sort((a, b) => b.precio - a.precio);
      case 'NOMBRE':
        return [...lista].sort((a, b) => a.titulo.localeCompare(b.titulo, 'es'));
      default:
        return [...lista].sort((a, b) => b.id - a.id);
    }
  });

  readonly hayFiltros = computed(
    () => this.busqueda() !== '' || this.tipo() !== 'TODOS' || this.categoria() !== 'TODAS',
  );

  ngOnInit() {
    // Los filtros viven en la URL: se pueden compartir y sobreviven a recargar.
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((p) => {
      this.busqueda.set(p.get('q') ?? '');
      const tipo = p.get('tipo');
      this.tipo.set(tipo === 'GRATUITO' || tipo === 'PAGO' ? tipo : 'TODOS');
      this.categoria.set(p.get('categoria') ?? 'TODAS');
      const orden = p.get('orden');
      this.ordenar.set(orden === 'NOMBRE' || orden === 'PRECIO_MENOR' || orden === 'PRECIO_MAYOR' ? orden : 'RECIENTES');
    });
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.api.publicados().subscribe({
      next: (r) => {
        this.cursos.set(r);
        this.error.set('');
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeError(e));
        this.cargando.set(false);
      },
    });
  }

  actualizar(cambio: Partial<{ q: string; tipo: Tipo; categoria: string; orden: Orden }>) {
    if (cambio.orden) this.ordenar.set(cambio.orden);
    if (cambio.q !== undefined) this.busqueda.set(cambio.q);
    if (cambio.tipo) this.tipo.set(cambio.tipo);
    if (cambio.categoria) this.categoria.set(cambio.categoria);
    void this.router.navigate([], {
      relativeTo: this.route,
      replaceUrl: true,
      queryParams: {
        orden: this.ordenar() === 'RECIENTES' ? null : this.ordenar(),
        q: this.busqueda() || null,
        tipo: this.tipo() === 'TODOS' ? null : this.tipo(),
        categoria: this.categoria() === 'TODAS' ? null : this.categoria(),
      },
    });
  }

  limpiarFiltros() {
    this.actualizar({ q: '', tipo: 'TODOS', categoria: 'TODAS' });
  }
}
