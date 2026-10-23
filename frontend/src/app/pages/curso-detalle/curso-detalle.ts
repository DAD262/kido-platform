import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Api, mensajeError } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { AccountContext } from '../../core/services/account-context';
import { ContenidoCurso, Curso } from '../../core/models/api.models';
import { SolesPipe } from '../../core/pipes/estado.pipe';
import { irACheckout } from '../../core/utils/checkout';
import { Icon } from '../../shared/icon/icon';
@Component({
  selector: 'app-curso-detalle',
  standalone: true,
  imports: [RouterLink, SolesPipe, Icon],
  templateUrl: './curso-detalle.html',
  styleUrl: './curso-detalle.scss',
})
export class CursoDetalle implements OnInit {
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  readonly auth = inject(Auth);
  readonly account = inject(AccountContext);
  readonly curso = signal<Curso | null>(null);
  readonly contenido = signal<ContenidoCurso | null>(null);
  readonly cargando = signal(true);
  readonly enviando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly lecciones = computed(
    () => this.contenido()?.modulos.reduce((n, m) => n + m.lecciones.length, 0) ?? 0,
  );
  readonly materiales = computed(
    () =>
      this.contenido()?.modulos.reduce(
        (n, m) => n + m.lecciones.reduce((v, l) => v + l.materiales.length, 0),
        0,
      ) ?? 0,
  );
  readonly tono = computed(() => Math.abs(this.curso()?.id ?? 0) % 4);
  ngOnInit() {
    // Resuelve el número de estudiante en segundo plano para poder inscribirse sin esperas.
    if (this.auth.tieneRol('ESTUDIANTE')) void this.account.asegurar();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(id) || id <= 0) {
      this.error.set('Identificador de curso inválido.');
      this.cargando.set(false);
      return;
    }
    forkJoin({ curso: this.api.curso(id), contenido: this.api.contenido(id) }).subscribe({
      next: (v) => {
        this.curso.set(v.curso);
        this.contenido.set(v.contenido);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeError(e));
        this.cargando.set(false);
      },
    });
  }
  inscribirme() {
    const curso = this.curso(),
      contenido = this.contenido();
    if (!curso || !contenido) return;
    if (!this.auth.autenticado) {
      void this.auth.login('/cursos/' + curso.id);
      return;
    }
    if (!this.auth.tieneRol('ESTUDIANTE')) {
      this.error.set('Para inscribirte debes ingresar con una cuenta de estudiante.');
      return;
    }
    const estudianteId = this.account.id();
    if (!estudianteId) {
      this.error.set(
        this.account.estado() === 'cargando'
          ? 'Estamos cargando tu perfil; vuelve a intentarlo en un segundo.'
          : 'No pudimos cargar tu perfil de estudiante. Ábrelo en «Mi aprendizaje» y vuelve a intentarlo.',
      );
      void this.account.asegurar(true);
      return;
    }
    if (curso.estado !== 'PUBLICADO') {
      this.error.set('Solo puedes inscribirte en cursos publicados.');
      return;
    }
    this.error.set('');
    this.mensaje.set('');
    this.enviando.set(true);
    if (curso.tipo === 'GRATUITO') {
      const ids = contenido.modulos.flatMap((m) => m.lecciones.map((l) => l.id));
      this.api.crearInscripcion(estudianteId, curso.id, ids, contenido.configuracion).subscribe({
        next: () => {
          this.enviando.set(false);
          this.mensaje.set('¡Inscripción creada! Ve a tu panel para comenzar.');
        },
        error: (e) => {
          this.enviando.set(false);
          this.error.set(mensajeError(e));
        },
      });
    } else {
      this.api.crearOrden(estudianteId, curso.id).subscribe({
        next: (orden) => {
          this.api.iniciarPago(orden.id).subscribe({
            next: (p) => {
              if (irACheckout(p, { tipo: 'curso', ordenId: orden.id, cursoId: curso.id })) {
                this.mensaje.set('Te llevamos a Mercado Pago para completar el pago…');
              } else {
                this.enviando.set(false);
                this.error.set(
                  `Orden #${orden.id} creada, pero Mercado Pago no devolvió un enlace de pago válido. Revisa el Access Token.`,
                );
              }
            },
            error: (e) => {
              this.enviando.set(false);
              this.error.set(
                `Orden #${orden.id} creada, pero no pudo iniciarse Mercado Pago: ${mensajeError(e)}`,
              );
            },
          });
        },
        error: (e) => {
          this.enviando.set(false);
          this.error.set(mensajeError(e));
        },
      });
    }
  }
}
