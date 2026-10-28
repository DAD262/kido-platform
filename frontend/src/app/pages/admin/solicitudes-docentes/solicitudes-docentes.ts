import { Component, inject, OnDestroy, OnInit, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { SolicitudDocente } from '../../../core/models/api.models';
import { KIDO_PIPES } from '../../../core/pipes/estado.pipe';
import { Api, mensajeError } from '../../../core/services/api';
import { etiquetaGrado, pesoLegible } from '../../../core/utils/registro';
import { Icon } from '../../../shared/icon/icon';

type Filtro = 'ACTIVAS' | 'APROBADA' | 'RECHAZADA' | 'TODAS';
type Accion = 'aprobar' | 'observar' | 'rechazar';

/**
 * Bandeja de verificación docente. Aprobar asigna el rol DOCENTE en Keycloak
 * (lo hace kido-usuario-ms con su cuenta de servicio); observar y rechazar exigen un motivo.
 */
@Component({
  selector: 'app-solicitudes-docentes',
  standalone: true,
  imports: [FormsModule, Icon, KIDO_PIPES],
  templateUrl: './solicitudes-docentes.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './solicitudes-docentes.scss',
})
export class SolicitudesDocentes implements OnInit, OnDestroy {
  private readonly api = inject(Api);
  private readonly sanitizer = inject(DomSanitizer);
  /** Avisa al panel para actualizar el contador de la pestaña. */
  readonly cambio = output<void>();

  readonly filtros: { id: Filtro; etiqueta: string }[] = [
    { id: 'ACTIVAS', etiqueta: 'Por revisar' },
    { id: 'APROBADA', etiqueta: 'Aprobadas' },
    { id: 'RECHAZADA', etiqueta: 'Rechazadas' },
    { id: 'TODAS', etiqueta: 'Todas' },
  ];
  readonly filtro = signal<Filtro>('ACTIVAS');
  readonly lista = signal<SolicitudDocente[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly mensaje = signal('');

  readonly abierta = signal<SolicitudDocente | null>(null);
  readonly pdfUrl = signal<SafeResourceUrl | null>(null);
  readonly cargandoPdf = signal(false);
  readonly procesando = signal<Accion | null>(null);
  readonly errorDetalle = signal('');
  private urlCruda: string | null = null;
  comentario = '';
  checks = { identidad: false, documento: false, presentacion: false };

  readonly grado = etiquetaGrado;
  readonly peso = pesoLegible;

  ngOnInit(): void {
    this.cargar();
  }

  ngOnDestroy(): void {
    this.liberarPdf();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.api.solicitudesDocente(this.filtro()).subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }

  filtrar(f: Filtro): void {
    if (this.filtro() === f) return;
    this.filtro.set(f);
    this.cargar();
  }

  iniciales(nombre: string): string {
    return (
      nombre
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((p) => p[0]?.toUpperCase())
        .join('') || '?'
    );
  }

  abrir(s: SolicitudDocente): void {
    this.abierta.set(s);
    this.comentario = '';
    this.checks = { identidad: false, documento: false, presentacion: false };
    this.errorDetalle.set('');
    this.liberarPdf();
    if (s.documento) this.cargarPdf(s.id);
  }

  cerrar(): void {
    this.abierta.set(null);
    this.liberarPdf();
  }

  revisable(s: SolicitudDocente): boolean {
    return s.estado === 'PENDIENTE' || s.estado === 'OBSERVADA';
  }

  verificado(): boolean {
    return this.checks.identidad && this.checks.documento && this.checks.presentacion;
  }

  private cargarPdf(id: number): void {
    this.cargandoPdf.set(true);
    this.api.documentoSolicitud(id).subscribe({
      next: (blob) => {
        if (this.abierta()?.id !== id) return;
        this.urlCruda = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
        // Es un blob: propio, generado a partir de la respuesta del API (no una URL externa).
        this.pdfUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.urlCruda));
        this.cargandoPdf.set(false);
      },
      error: (e) => {
        this.cargandoPdf.set(false);
        this.errorDetalle.set(mensajeError(e));
      },
    });
  }

  abrirPdfAparte(): void {
    if (this.urlCruda) window.open(this.urlCruda, '_blank', 'noopener');
  }

  private liberarPdf(): void {
    if (this.urlCruda) URL.revokeObjectURL(this.urlCruda);
    this.urlCruda = null;
    this.pdfUrl.set(null);
  }

  decidir(accion: Accion): void {
    const s = this.abierta();
    if (!s || this.procesando()) return;
    if (accion !== 'aprobar' && this.comentario.trim().length < 10) {
      this.errorDetalle.set(
        'Escribe al menos 10 caracteres para que el postulante sepa qué corregir o por qué se rechaza.',
      );
      return;
    }
    if (accion === 'aprobar' && !this.verificado()) {
      this.errorDetalle.set('Marca las tres verificaciones antes de aprobar.');
      return;
    }
    this.procesando.set(accion);
    this.errorDetalle.set('');
    this.api.revisarSolicitud(s.id, accion, this.comentario).subscribe({
      next: (r) => {
        this.procesando.set(null);
        this.cerrar();
        this.mensaje.set(
          accion === 'aprobar'
            ? `${r.nombreCompleto} ya es docente. Verá su panel al volver a iniciar sesión o actualizar su sesión.`
            : accion === 'observar'
              ? `Se pidió a ${r.nombreCompleto} que corrija su postulación.`
              : `La postulación de ${r.nombreCompleto} fue rechazada.`,
        );
        this.cargar();
        this.cambio.emit();
      },
      error: (e) => {
        this.procesando.set(null);
        this.errorDetalle.set(mensajeError(e));
      },
    });
  }
}
