import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, catchError, of, Observable } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../core/services/auth';
import { Api, mensajeError } from '../../core/services/api';
import {
  ContenidoCurso,
  Curso,
  Material,
  Orden,
  Reembolso,
  ResumenSolicitudes,
  Retiro,
} from '../../core/models/api.models';
import { KEYCLOAK_ADMIN_URL } from '../../core/config/keycloak.config';
import { KIDO_PIPES } from '../../core/pipes/estado.pipe';
import { Icon } from '../../shared/icon/icon';
import { SolicitudesDocentes } from './solicitudes-docentes/solicitudes-docentes';
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [RouterLink, FormsModule, Icon, KIDO_PIPES, SolicitudesDocentes],
  templateUrl: './admin.html',
  styleUrl: './admin.scss',
})
export class Admin implements OnInit {
  readonly auth = inject(Auth);
  private readonly api = inject(Api);
  readonly keycloakAdmin = KEYCLOAK_ADMIN_URL;
  hostActual() {
    return window.location.hostname;
  }
  readonly cursos = signal<Curso[]>([]);
  readonly ordenes = signal<Orden[]>([]);
  readonly retiros = signal<Retiro[]>([]);
  readonly reembolsos = signal<Reembolso[]>([]);
  readonly resumenDocentes = signal<ResumenSolicitudes | null>(null);
  readonly cargando = signal(true);
  readonly procesando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly tab = signal<'cursos' | 'finanzas' | 'usuarios'>('cursos');
  readonly busqueda = signal('');
  readonly filtroFinanzas = signal('TODAS');
  readonly vistaPrevia = signal<Curso | null>(null);
  readonly contenidoVista = signal<ContenidoCurso | null>(null);
  readonly cargandoVista = signal(false);
  readonly errorVista = signal('');
  readonly fallos = signal<string[]>([]);
  readonly dialogo = signal<{
    grupo: 'curso' | 'retiro' | 'reembolso';
    id: number;
    accion: 'aprobar' | 'rechazar' | 'pagar';
  } | null>(null);
  dato = '';
  confirmado = false;
  private parcial<T>(nombre: string, op: Observable<T>, vacio: T) {
    return op.pipe(
      catchError((e) => {
        this.fallos.update((v) => [...v, `${nombre}: ${mensajeError(e)}`]);
        return of(vacio);
      }),
    );
  }
  necesitaDato() {
    const d = this.dialogo();
    return d?.accion === 'rechazar' || d?.accion === 'pagar';
  }
  tituloDialogo() {
    const d = this.dialogo();
    if (!d) return '';
    const objeto = { curso: 'el curso', retiro: 'el retiro', reembolso: 'el reembolso' }[d.grupo];
    if (d.grupo === 'curso' && d.accion === 'rechazar') return 'Pedir cambios al docente';
    if (d.accion === 'pagar') return `Registrar la transferencia del retiro n.º ${d.id}`;
    return `${d.accion === 'aprobar' ? 'Aprobar' : 'Rechazar'} ${objeto} n.º ${d.id}`;
  }

  abrirDialogo(
    grupo: 'curso' | 'retiro' | 'reembolso',
    id: number,
    accion: 'aprobar' | 'rechazar' | 'pagar',
  ) {
    if (this.procesando()) return;
    this.dato = '';
    this.confirmado = false;
    this.error.set('');
    this.dialogo.set({ grupo, id, accion });
  }
  confirmarDialogo() {
    const d = this.dialogo();
    if (
      !d ||
      this.procesando() ||
      (this.necesitaDato() && !this.dato.trim()) ||
      (d.accion === 'pagar' && !this.confirmado)
    )
      return;
    const op: Observable<unknown> =
      d.grupo === 'curso'
        ? d.accion === 'aprobar'
          ? this.api.aprobarCurso(d.id)
          : this.api.rechazarCurso(d.id, this.dato.trim())
        : d.grupo === 'retiro'
          ? d.accion === 'aprobar'
            ? this.api.aprobarRetiro(d.id)
            : d.accion === 'pagar'
              ? this.api.pagarRetiro(d.id, this.dato.trim())
              : this.api.rechazarRetiro(d.id, this.dato.trim())
          : d.accion === 'aprobar'
            ? this.api.aprobarReembolso(d.id)
            : this.api.rechazarReembolso(d.id, this.dato.trim());
    this.procesando.set(true);
    this.error.set('');
    op.subscribe({
      next: () => {
        this.procesando.set(false);
        this.dialogo.set(null);
        this.mensaje.set('Operación registrada correctamente.');
        this.cargar();
      },
      error: (e) => {
        this.procesando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
  ngOnInit() {
    this.cargar();
  }
  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.fallos.set([]);
    forkJoin({
      cursos: this.parcial('Cursos', this.api.cursos(), []),
      ordenes: this.parcial('Órdenes', this.api.ordenes(), []),
      retiros: this.parcial('Retiros', this.api.retiros(), []),
      reembolsos: this.parcial('Reembolsos', this.api.reembolsos(), []),
      docentes: this.parcial<ResumenSolicitudes | null>(
        'Cuentas',
        this.api.resumenSolicitudes(),
        null,
      ),
    }).subscribe({
      next: (v) => {
        this.resumenDocentes.set(v.docentes);
        this.cursos.set(v.cursos);
        this.ordenes.set(v.ordenes);
        this.retiros.set(v.retiros);
        this.reembolsos.set(v.reembolsos);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
  docentesPendientes() {
    return this.resumenDocentes()?.pendientes ?? 0;
  }
  cargarResumenDocentes() {
    this.api
      .resumenSolicitudes()
      .subscribe({ next: (r) => this.resumenDocentes.set(r), error: () => {} });
  }
  pendientes() {
    return this.cursos().filter((c) => c.estado === 'PENDIENTE_REVISION');
  }
  cursosFiltrados() {
    const q = this.busqueda().trim().toLocaleLowerCase();
    return this.cursos().filter(
      (c) =>
        !q ||
        `${c.titulo} ${c.descripcion} ${c.categoria} ${c.estado}`.toLocaleLowerCase().includes(q),
    );
  }
  ordenesFiltradas() {
    return this.ordenes().filter(
      (o) => this.filtroFinanzas() === 'TODAS' || o.estado === this.filtroFinanzas(),
    );
  }
  estadosOrden() {
    return [...new Set(this.ordenes().map((o) => o.estado))].sort();
  }
  totalAprobado() {
    return this.ordenes()
      .filter((o) => o.estado === 'APROBADA')
      .reduce((s, o) => s + (Number(o.montoTotal) || 0), 0);
  }
  comisionKido() {
    return this.ordenes()
      .filter((o) => o.estado === 'APROBADA')
      .reduce((s, o) => s + (Number(o.comisionKido) || 0), 0);
  }
  tieneLeccionesRevisables() {
    return !!this.contenidoVista()?.modulos.some((m) => m.lecciones.length > 0);
  }
  retirarPendientes() {
    return this.retiros().filter((r) => r.estado === 'PENDING').length;
  }
  reembolsosPendientes() {
    return this.reembolsos().filter((r) => r.estado === 'SOLICITADO').length;
  }
  abrirRevision(c: Curso) {
    this.vistaPrevia.set(c);
    this.contenidoVista.set(null);
    this.errorVista.set('');
    this.cargandoVista.set(true);
    this.api.contenido(c.id).subscribe({
      next: (r) => {
        if (this.vistaPrevia()?.id === c.id) {
          this.contenidoVista.set(r);
          this.cargandoVista.set(false);
        }
      },
      error: (e) => {
        if (this.vistaPrevia()?.id === c.id) {
          this.errorVista.set(mensajeError(e));
          this.cargandoVista.set(false);
        }
      },
    });
  }
  cerrarRevision() {
    this.vistaPrevia.set(null);
    this.contenidoVista.set(null);
    this.errorVista.set('');
  }
  revisarYActuar(accion: 'aprobar' | 'rechazar') {
    const c = this.vistaPrevia();
    if (!c || this.cargandoVista() || !this.contenidoVista()) return;
    this.cerrarRevision();
    this.abrirDialogo('curso', c.id, accion);
  }
  descargarMaterial(mat: Material) {
    if (/^https?:\/\//i.test(mat.url)) {
      window.open(mat.url, '_blank', 'noopener,noreferrer');
      return;
    }
    if (!mat.url?.startsWith('/api/v1/materiales/archivos/')) {
      this.errorVista.set('Este recurso no tiene una URL de archivo compatible.');
      return;
    }
    this.api.descargarMaterial(mat.url).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = mat.nombre || 'material-kido';
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: (e) => this.errorVista.set(mensajeError(e)),
    });
  }

  ejecutarCurso(id: number, accion: 'aprobar' | 'rechazar') {
    this.abrirDialogo('curso', id, accion);
  }
  ejecutarRetiro(id: number, accion: 'aprobar' | 'pagar' | 'rechazar') {
    this.abrirDialogo('retiro', id, accion);
  }
  ejecutarReembolso(id: number, accion: 'aprobar' | 'rechazar') {
    this.abrirDialogo('reembolso', id, accion);
  }
  cerrarSesion() {
    void this.auth.logout();
  }
}
