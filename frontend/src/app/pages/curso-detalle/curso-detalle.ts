import { portadaCurso } from '../../core/utils/curso-portada';
import { CertificadoModelo } from '../../shared/certificado-modelo/certificado-modelo';
import { Component, computed, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, map, switchMap } from 'rxjs';
import { Api, mensajeError } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { Deseados } from '../../core/services/deseados';
import { Carrito } from '../../core/services/carrito';
import { Docentes } from '../../core/services/docentes';
import { AccountContext } from '../../core/services/account-context';
import { ContenidoCurso, Curso } from '../../core/models/api.models';
import { SolesPipe } from '../../core/pipes/estado.pipe';
import { irACheckout } from '../../core/utils/checkout';
import { CheckoutCompra, EstadoCheckout } from '../../shared/checkout-compra/checkout-compra';
import { Icon } from '../../shared/icon/icon';
@Component({
  selector: 'app-curso-detalle',
  standalone: true,
  imports: [RouterLink, SolesPipe, Icon, CheckoutCompra, CertificadoModelo],
  templateUrl: './curso-detalle.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './curso-detalle.scss',
})
export class CursoDetalle implements OnInit {
  readonly docentes=inject(Docentes);
  readonly carrito=inject(Carrito);
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  readonly auth = inject(Auth);
  readonly deseados = inject(Deseados);
  readonly account = inject(AccountContext);
  readonly curso = signal<Curso | null>(null);
  readonly contenido = signal<ContenidoCurso | null>(null);
  readonly cargando = signal(true);
  readonly enviando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly checkout=signal<EstadoCheckout>({disponible:false,acepta:false,version:0});
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
  readonly portada = computed(() => portadaCurso(this.curso()?.titulo));
  readonly tono = computed(() => Math.abs(this.curso()?.id ?? 0) % 4);
  ngOnInit() {
    this.docentes.cargar();this.carrito.cargar();
    this.deseados.cargar();
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
    if (curso.tipo !== 'GRATUITO' && (!this.checkout().disponible || !this.checkout().acepta)) {
      this.error.set('Comprueba la disponibilidad y acepta las condiciones antes de continuar.');
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
      this.api.crearOrden(estudianteId, curso.id).pipe(
        switchMap(orden => this.api.iniciarPago(orden.id, {acepta:this.checkout().acepta,version:this.checkout().version}).pipe(
          map(preferencia => ({ orden, preferencia })),
        )),
      ).subscribe({
        next: ({ orden, preferencia }) => {
          if (irACheckout(preferencia, { tipo: 'curso', ordenId: orden.id, cursoId: curso.id })) {
            this.mensaje.set('Te llevamos a Mercado Pago para completar el pago…');
          } else {
            this.enviando.set(false);
            this.error.set('No recibimos un enlace de pago válido. Vuelve a intentarlo; no se ha confirmado ningún cobro.');
          }
        },
        error: e => {
          this.enviando.set(false);
          this.error.set(mensajeError(e));
        },
      });
    }
  }
}
