import { LiquidacionesPanel } from '../../shared/liquidaciones-panel/liquidaciones-panel';
import { Component, computed, inject, OnInit, signal, ChangeDetectionStrategy,DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, catchError, of, Observable,finalize,timeout,TimeoutError,Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpEventType } from '@angular/common/http';
import { Api, mensajeError } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { AccountContext } from '../../core/services/account-context';
import {
  ConfigCurso,
  ContenidoCurso,
  Modulo,
  Leccion,
  Material,
  Curso,
  CursoRequest,
  Inscripcion,
  Orden,
  Retiro,
  Saldo,
} from '../../core/models/api.models';
import { KIDO_PIPES } from '../../core/pipes/estado.pipe';
import { Icon } from '../../shared/icon/icon';
import { PerfilEstado } from '../../shared/perfil-estado/perfil-estado';
@Component({
  selector: 'app-docente',
  standalone: true,
  imports: [LiquidacionesPanel, FormsModule, RouterLink, Icon, KIDO_PIPES, PerfilEstado],
  templateUrl: './docente.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './docente.scss',
})
export class Docente implements OnInit {
  private readonly destroyRef=inject(DestroyRef);
  private lecturaContenido?:Subscription;
  readonly cargandoContenido=signal(false);
  readonly guardandoLeccion=signal(false);
  readonly auth = inject(Auth);
  readonly account = inject(AccountContext);
  private readonly api = inject(Api);
  readonly tab = signal<'cursos' | 'alumnos' | 'finanzas'>('cursos');
  readonly cursos = signal<Curso[]>([]);
  readonly seleccionado = signal<Curso | null>(null);
  readonly alumnos = signal<Inscripcion[]>([]);
  readonly contenido = signal<ContenidoCurso | null>(null);
  readonly saldo = signal<Saldo | null>(null);
  readonly ordenes = signal<Orden[]>([]);
  readonly retiros = signal<Retiro[]>([]);
  readonly cargando = signal(false);
  readonly procesando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly editar = signal(false);
  readonly editorPaso = signal<0 | 1 | 2 | 3 | 4>(0);
  /** El editor ocupa todo el ancho; la lista de cursos se oculta mientras se edita. */
  readonly enEditor = signal(false);
  readonly pasos: { n: 0 | 1 | 2 | 3 | 4; texto: string }[] = [
    { n: 0, texto: 'Datos' },
    { n: 1, texto: 'Módulos' },
    { n: 2, texto: 'Lecciones' },
    { n: 3, texto: 'Materiales' },
    { n: 4, texto: 'Requisitos' },
  ];
  readonly estadosCurso = ['BORRADOR', 'PENDIENTE_REVISION', 'PUBLICADO', 'RECHAZADO', 'ARCHIVADO'];
  readonly publicados = computed(
    () => this.cursos().filter((c) => c.estado === 'PUBLICADO').length,
  );
  pasoCompleto(n: number): boolean {
    const c = this.contenido();
    switch (n) {
      case 0:
        return !!this.seleccionado();
      case 1:
        return !!c?.modulos.length;
      case 2:
        return this.leccionesTotal() > 0;
      case 3:
        return this.materialesTotal() > 0;
      default:
        return ['PENDIENTE_REVISION', 'PUBLICADO'].includes(this.seleccionado()?.estado ?? '');
    }
  }
  cerrarEditor() {
    if (this.procesando()) return;
    this.enEditor.set(false);
    this.cargar();
  }
  irPaso(paso: 0 | 1 | 2 | 3 | 4) {
    if (paso !== 0 && !this.seleccionado()) return;
    this.editorPaso.set(paso);
  }
  pasoSiguiente() {
    const p = this.editorPaso();
    if (p < 4) this.irPaso((p + 1) as 0 | 1 | 2 | 3 | 4);
  }
  readonly avance = signal<number | null>(null);
  readonly fallos = signal<string[]>([]);
  readonly dialogo = signal<{
    tipo: 'modulo' | 'leccion' | 'eliminarModulo' | 'eliminarLeccion' | 'eliminarMaterial';
    id: number;
    titulo: string;
    orden: number;
    url: string;
  } | null>(null);
  busquedaCurso = '';
  busquedaAlumno = '';
  filtroEstado = '';
  private lectura = 0;
  private carga = 0;
  cursosFiltrados() {
    return this.cursos().filter(
      (c) =>
        c.titulo.toLowerCase().includes(this.busquedaCurso.toLowerCase()) &&
        (!this.filtroEstado || c.estado === this.filtroEstado),
    );
  }
  alumnosFiltrados() {
    return this.alumnos().filter((a) =>
      `${a.id} ${a.estudianteId} ${a.estado}`
        .toLowerCase()
        .includes(this.busquedaAlumno.toLowerCase()),
    );
  }
  leccionesTotal() {
    return this.contenido()?.modulos.reduce((n, m) => n + m.lecciones.length, 0) ?? 0;
  }
  materialesTotal() {
    return (
      this.contenido()?.modulos.reduce(
        (n, m) => n + m.lecciones.reduce((v, l) => v + l.materiales.length, 0),
        0,
      ) ?? 0
    );
  }
  private parcial<T>(nombre: string, op: Observable<T>, vacio: T) {
    return op.pipe(
      timeout(15000),
      catchError((e) => {
        this.fallos.update((v) => [...v, `${nombre}: ${e instanceof TimeoutError?'La solicitud tardó demasiado. Puedes reintentar.':mensajeError(e)}`]);
        return of(vacio);
      }),
    );
  }
  exportarAlumnos() {
    const filas = [
      ['Inscripcion', 'Estudiante', 'Progreso', 'Estado'],
      ...this.alumnosFiltrados().map((a) => [a.id, a.estudianteId, a.porcentajeProgreso, a.estado]),
    ];
    const csv =
      '\uFEFF' +
      filas
        .map((f) => f.map((v) => '"' + String(v).replace(/"/g, '""') + '"').join(';'))
        .join('\r\n');
    this.descargarBlob(
      new Blob([csv], { type: 'text/csv;charset=utf-8' }),
      `inscritos-curso-${this.seleccionado()?.id}.csv`,
    );
  }
  private descargarBlob(blob: Blob, nombre: string) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  abrirMaterial(m: Material) {
    if (m.url.startsWith('/api/v1/materiales/archivos/'))
      this.api.descargarMaterial(m.url).subscribe({
        next: (b) => this.descargarBlob(b, m.nombre),
        error: (e) => this.error.set(mensajeError(e)),
      });
    else if (/^https:\/\//i.test(m.url)) window.open(m.url, '_blank', 'noopener,noreferrer');
  }
  editarModulo(m: Modulo) {
    this.dialogo.set({ tipo: 'modulo', id: m.id, titulo: m.titulo, orden: m.orden, url: '' });
  }
  editarLeccion(l: Leccion) {
    this.dialogo.set({
      tipo: 'leccion',
      id: l.id,
      titulo: l.titulo,
      orden: l.orden,
      url: l.urlContenido || '',
    });
  }
  eliminar(
    tipo: 'eliminarModulo' | 'eliminarLeccion' | 'eliminarMaterial',
    id: number,
    titulo: string,
  ) {
    this.dialogo.set({ tipo, id, titulo, orden: 1, url: '' });
  }
  cerrarDialogo() {
    if (!this.procesando()) this.dialogo.set(null);
  }
  confirmarDialogo() {
    const d = this.dialogo(),
      curso = this.seleccionado()?.id;
    if (!d || !curso || this.procesando()) return;
    if (
      (d.tipo === 'modulo' || d.tipo === 'leccion') &&
      (!d.titulo.trim() || !Number.isInteger(+d.orden) || +d.orden < 1)
    ) {
      this.error.set('Escribe un título y una posición entera mayor que cero.');
      return;
    }
    if (d.tipo === 'leccion' && d.url && !/^https?:\/\//i.test(d.url)) {
      this.error.set('El enlace debe comenzar con https:// o http://.');
      return;
    }
    const op: Observable<unknown> =
      d.tipo === 'modulo'
        ? this.api.actualizarModulo(d.id, d.titulo.trim(), +d.orden)
        : d.tipo === 'leccion'
          ? this.api.actualizarLeccion(d.id, d.titulo.trim(), +d.orden, d.url)
          : d.tipo === 'eliminarModulo'
            ? this.api.eliminarModulo(d.id)
            : d.tipo === 'eliminarLeccion'
              ? this.api.eliminarLeccion(d.id)
              : this.api.eliminarMaterial(d.id);
    this.procesando.set(true);
    this.error.set('');
    op.subscribe({
      next: () => {
        this.procesando.set(false);
        this.dialogo.set(null);
        this.mensaje.set('Contenido actualizado.');
        this.verContenido(curso);
      },
      error: (e) => {
        this.procesando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }

  formulario: CursoRequest = {
    titulo: '',
    descripcion: '',
    tipo: 'GRATUITO',
    precio: 0,
    docenteId: 1,
    categoriaId: 1,
    estado: 'BORRADOR',
  };
  moduloTitulo = '';
  moduloOrden = 1;
  leccionTitulo = '';
  leccionOrden = 1;
  leccionUrl = '';
  archivo: File | null = null;
  archivoLeccionId: number | null = null;
  moduloId: number | null = null;
  retiroMonto = 50;
  retiroMedio = 'CUENTA_BANCARIA';
  retiroDestino = '';
  config: Omit<ConfigCurso, 'cursoId'> = {
    progresoMinimo: 100,
    asistenciaMinima: 0,
    certificadoHabilitado: false,
    certificadoCosto: 0,
  };
  ngOnInit() {
    // El número de docente llega de GET /usuarios/me (sin vincular nada a mano).
    void this.account.asegurar().then(() => {
      if (this.account.id()) this.cargar();
    });
  }
  cargar() {
    const id = this.account.id();
    if (!id || this.cargando()) return;
    const version = ++this.carga;
    this.error.set('');
    this.fallos.set([]);
    this.cargando.set(true);
    this.formulario.docenteId = id;
    forkJoin({
      cursos: this.parcial('Cursos', this.api.cursosDocente(id), []),
      ordenes: this.parcial('Órdenes', this.api.ordenesDocente(id), []),
      saldo: this.parcial<Saldo | null>('Saldo', this.api.saldoDocente(id), null),
      retiros: this.parcial('Retiros', this.api.retirosDocente(id), []),
    }).pipe(takeUntilDestroyed(this.destroyRef),finalize(()=>{if(version===this.carga)this.cargando.set(false);})).subscribe((v) => {
      if (version !== this.carga) return;
      this.cursos.set(v.cursos);
      this.ordenes.set(v.ordenes);
      this.saldo.set(v.saldo);
      this.retiros.set(v.retiros);
      this.cargando.set(false);
    });
  }
  nuevo() {
    if (this.procesando()) return;
    this.enEditor.set(true);
    this.editorPaso.set(0);
    ++this.lectura;
    this.moduloId = null;
    this.archivoLeccionId = null;
    this.archivo = null;
    this.moduloTitulo = '';
    this.leccionTitulo = '';
    this.editar.set(false);
    this.seleccionado.set(null);
    this.contenido.set(null);
    this.formulario = {
      titulo: '',
      descripcion: '',
      tipo: 'GRATUITO',
      precio: 0,
      docenteId: this.account.id() || 1,
      categoriaId: 1,
      estado: 'BORRADOR',
    };
  }
  editarCurso(c: Curso) {
    if (this.procesando()) return;
    this.enEditor.set(true);
    this.editorPaso.set(0);
    this.contenido.set(null);
    this.moduloId = null;
    this.archivoLeccionId = null;
    this.archivo = null;
    this.seleccionado.set(c);
    this.editar.set(true);
    this.formulario = {
      titulo: c.titulo,
      descripcion: c.descripcion,
      tipo: c.tipo,
      precio: c.precio,
      docenteId: c.docenteId,
      categoriaId: c.categoriaId,
      estado: c.estado,
    };
    this.verContenido(c.id);
  }
  guardarCurso() {
    const id = this.account.id();
    if (!id || this.procesando()) return;
    this.formulario.titulo = this.formulario.titulo.trim();
    this.formulario.descripcion = this.formulario.descripcion.trim();
    if (
      !this.formulario.titulo ||
      !this.formulario.descripcion ||
      (this.formulario.tipo === 'PAGO' &&
        (!Number.isFinite(+this.formulario.precio) || +this.formulario.precio <= 0))
    ) {
      this.error.set(
        'Título y descripción obligatorios; un curso de pago necesita un precio mayor que cero.',
      );
      return;
    }
    this.formulario.docenteId = id;
    if (this.formulario.tipo === 'GRATUITO') this.formulario.precio = 0;
    this.procesando.set(true);
    this.error.set('');
    this.mensaje.set('');
    const op =
      this.editar() && this.seleccionado()
        ? this.api.actualizarCurso(this.seleccionado()!.id, this.formulario)
        : this.api.crearCurso(this.formulario);
    op.subscribe({
      next: (r) => {
        this.procesando.set(false);
        this.mensaje.set(
          `Curso #${r.id} guardado. Agrega módulos y lecciones antes de enviarlo a revisión.`,
        );
        this.editarCurso(r);
        this.editorPaso.set(1);
        this.api.cursosDocente(id).subscribe({ next: (c) => this.cursos.set(c) });
      },
      error: (e) => {
        this.procesando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
  accion(id: number, accion: 'revision' | 'archivar') {
    if (this.procesando()) return;
    this.procesando.set(true);
    this.error.set('');
    const op = accion === 'revision' ? this.api.revision(id) : this.api.archivarCurso(id);
    op.subscribe({
      next: (r) => {
        this.procesando.set(false);
        this.mensaje.set(`Curso #${id}: ${r.estado}`);
        this.cargar();
      },
      error: (e) => {
        this.procesando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
  verContenido(id: number) {
    const version = ++this.lectura;
    this.lecturaContenido?.unsubscribe();
    this.cargandoContenido.set(true);
    this.lecturaContenido=this.api.contenido(id).pipe(timeout(15000),takeUntilDestroyed(this.destroyRef),finalize(()=>{if(version===this.lectura)this.cargandoContenido.set(false);})).subscribe({
      next: (c) => {
        if (version !== this.lectura || this.seleccionado()?.id !== id) return;
        this.contenido.set(c);
        this.moduloOrden = Math.max(0, ...c.modulos.map((m) => m.orden)) + 1;
        const elegido=c.modulos.find(m=>m.id===Number(this.moduloId))??c.modulos[0];
        this.seleccionarModulo(elegido?.id??null);
        const lecciones=c.modulos.flatMap(m=>m.lecciones);
        if(!lecciones.some(l=>l.id===this.archivoLeccionId))this.archivoLeccionId=lecciones[0]?.id??null;
        this.config = {
          progresoMinimo: c.configuracion.progresoMinimo,
          asistenciaMinima: c.configuracion.asistenciaMinima,
          certificadoHabilitado: c.configuracion.certificadoHabilitado,
          certificadoCosto: c.configuracion.certificadoCosto,
        };
      },
      error: (e) => {if(version===this.lectura&&this.seleccionado()?.id===id)this.error.set(e instanceof TimeoutError?'La estructura tardó demasiado en cargar. Reintenta sin borrar lo que escribiste.':mensajeError(e));},
    });
  }
  seleccionarModulo(id: number | string | null) {
    const numero=Number(id);
    this.moduloId=Number.isSafeInteger(numero)&&numero>0?numero:null;
    const m = this.contenido()?.modulos.find((m) => m.id === this.moduloId);
    this.leccionOrden = Math.max(0, ...(m?.lecciones.map((l) => l.orden) ?? [])) + 1;
  }
  moduloSeleccionado(){return this.contenido()?.modulos.find(m=>m.id===this.moduloId);}
  crearModulo() {
    const id = this.seleccionado()?.id;
    if (this.procesando() || !id || !this.moduloTitulo.trim()) return;
    if (!Number.isInteger(+this.moduloOrden) || +this.moduloOrden < 1) {
      this.error.set('La posición debe ser un número entero mayor que cero.');
      return;
    }
    if (
      this.contenido()?.modulos.some(
        (m) =>
          m.orden === +this.moduloOrden ||
          m.titulo.toLowerCase() === this.moduloTitulo.trim().toLowerCase(),
      )
    ) {
      this.error.set(
        'Ya existe un módulo con ese título o posición. Edita el existente o cambia esos datos.',
      );
      return;
    }
    this.procesando.set(true);
    this.api.crearModulo(id, this.moduloTitulo.trim(), this.moduloOrden).pipe(timeout(20000),takeUntilDestroyed(this.destroyRef),finalize(()=>this.procesando.set(false))).subscribe({
      next: (creado) => {
        this.moduloId=creado.id;
        this.moduloTitulo = '';
        this.procesando.set(false);
        this.verContenido(id);
        this.editorPaso.set(2);
        this.mensaje.set('Módulo creado. Ya puedes agregar lecciones.');
      },
      error: (e) => {
        this.procesando.set(false);
        this.error.set(e instanceof TimeoutError?'No se confirmó el módulo a tiempo. Actualiza la estructura antes de reintentarlo.':mensajeError(e));
      },
    });
  }
  crearLeccion() {
    const id = this.seleccionado()?.id;
    if (this.procesando() || this.guardandoLeccion() || this.cargandoContenido()) return;
    const m=this.moduloSeleccionado();
    if(!id||!m){this.error.set('Selecciona un módulo válido o vuelve a cargar la estructura.');return;}
    if(!this.leccionTitulo.trim()){this.error.set('Escribe el título de la lección.');return;}
    if (!Number.isInteger(+this.leccionOrden) || +this.leccionOrden < 1) {
      this.error.set('La posición debe ser un número entero mayor que cero.');
      return;
    }
    if (
      m.lecciones.some(
        (l) =>
          l.orden === +this.leccionOrden ||
          l.titulo.toLowerCase() === this.leccionTitulo.trim().toLowerCase(),
      )
    ) {
      this.error.set('Ya existe una lección con ese título o posición en el módulo.');
      return;
    }
    this.leccionUrl=this.leccionUrl.trim();
    if (this.leccionUrl && !/^https?:\/\//i.test(this.leccionUrl)) {
      this.error.set('El enlace debe comenzar con https:// o http://.');
      return;
    }
    this.procesando.set(true);
    this.guardandoLeccion.set(true);
    this.error.set('');this.mensaje.set('');
    this.api
      .crearLeccion(m.id, this.leccionTitulo.trim(), Number(this.leccionOrden), this.leccionUrl.trim())
      .pipe(timeout(20000),takeUntilDestroyed(this.destroyRef),finalize(()=>{this.procesando.set(false);this.guardandoLeccion.set(false);}))
      .subscribe({
        next: (creada) => {
          this.archivoLeccionId=creada.id;
          this.leccionTitulo = '';
          this.leccionUrl = '';
          this.leccionOrden++;
          this.procesando.set(false);
          this.verContenido(id);
          this.editorPaso.set(3);
          this.mensaje.set('Lección creada. Ya puedes adjuntar materiales.');
        },
        error: (e) => {
          this.procesando.set(false);
          this.error.set(e instanceof TimeoutError?'No se confirmó la lección a tiempo. Conservamos tus datos; revisa la estructura antes de reintentar.':mensajeError(e));
          if(e instanceof TimeoutError)this.verContenido(id);
        },
      });
  }
  guardarConfig() {
    const id = this.seleccionado()?.id;
    if (!id || this.procesando()) return;
    if (!this.config.certificadoHabilitado) this.config.certificadoCosto = 0;
    if (
      !Number.isInteger(+this.config.progresoMinimo) ||
      !Number.isInteger(+this.config.asistenciaMinima) ||
      this.config.progresoMinimo < 0 ||
      this.config.progresoMinimo > 100 ||
      this.config.asistenciaMinima < 0 ||
      this.config.asistenciaMinima > 100 ||
      !Number.isFinite(+this.config.certificadoCosto) ||
      this.config.certificadoCosto < 0
    ) {
      this.error.set(
        'Los porcentajes deben ser enteros entre 0 y 100 y el costo no puede ser negativo.',
      );
      return;
    }
    this.procesando.set(true);
    this.api.configurarCurso(id, this.config).subscribe({
      next: () => {
        this.procesando.set(false);
        this.mensaje.set('Reglas académicas guardadas.');
        this.verContenido(id);
      },
      error: (e) => {
        this.procesando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
  verAlumnos(c: Curso) {
    if (this.procesando()) return;
    this.enEditor.set(false);
    ++this.lectura;
    this.contenido.set(null);
    this.alumnos.set([]);
    this.seleccionado.set(c);
    this.tab.set('alumnos');
    this.error.set('');
    this.api.inscripcionesCurso(c.id).subscribe({
      next: (r) => {
        if (this.seleccionado()?.id === c.id) this.alumnos.set(r);
      },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }
  elegirArchivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.archivo = input.files?.item(0) ?? null;
    this.error.set('');
    this.avance.set(null);
    if (
      this.archivo &&
      (this.archivo.size === 0 ||
        this.archivo.size > 50 * 1024 * 1024 ||
        !/\.(pdf|docx?|pptx?|xlsx?|txt|zip|png|jpe?g|webp|mp3|mp4|webm)$/i.test(this.archivo.name))
    ) {
      this.archivo = null;
      input.value = '';
      this.error.set('Elige un archivo permitido, no vacío y de hasta 50 MB.');
    }
  }

  subirArchivo(): void {
    const cursoId = this.seleccionado()?.id;
    const leccionId = this.archivoLeccionId;
    const archivo = this.archivo;

    if (this.procesando()) return;
    if (!cursoId || !leccionId || !archivo) {
      this.error.set('Selecciona una lección y un archivo antes de subir el material.');
      return;
    }

    this.error.set('');
    this.mensaje.set('');
    this.procesando.set(true);

    this.avance.set(0);
    this.api.subirMaterialProgreso(leccionId, archivo).subscribe({
      next: (evento) => {
        if (evento.type === HttpEventType.UploadProgress) {
          this.avance.set(evento.total ? Math.round((100 * evento.loaded) / evento.total) : null);
          return;
        }
        if (evento.type !== HttpEventType.Response) return;
        this.avance.set(100);
        const input = document.getElementById('kido-material-file') as HTMLInputElement | null;
        if (input) input.value = '';
        this.procesando.set(false);
        this.archivo = null;
        this.archivoLeccionId = null;
        this.mensaje.set('Material subido correctamente.');
        this.verContenido(cursoId);
      },
      error: (e: unknown) => {
        this.procesando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }

  seleccionarAlumnos(cursoId: number | null): void {
    this.enEditor.set(false);
    const curso = this.cursos().find((c) => c.id === Number(cursoId)) ?? null;
    ++this.lectura;
    this.contenido.set(null);
    this.seleccionado.set(curso);
    this.alumnos.set([]);
    this.error.set('');
    if (!curso) return;

    this.api.inscripcionesCurso(curso.id).subscribe({
      next: (alumnos) => {
        if (this.seleccionado()?.id === curso.id) this.alumnos.set(alumnos);
      },
      error: (e: unknown) => {
        if (this.seleccionado()?.id === curso.id) this.error.set(mensajeError(e));
      },
    });
  }

  solicitarRetiro() {
    const id = this.account.id();
    if (!id || this.procesando()) return;
    if (
      !this.saldo() ||
      this.fallos().some((f) => f.startsWith('Saldo:')) ||
      !Number.isFinite(+this.retiroMonto) ||
      +this.retiroMonto < 50 ||
      +this.retiroMonto > this.saldo()!.disponible ||
      !this.retiroDestino.trim()
    ) {
      this.error.set(
        'Revisa el saldo: mínimo S/ 50, monto no mayor al disponible y destino obligatorio.',
      );
      return;
    }
    this.procesando.set(true);
    this.error.set('');
    this.api
      .solicitarRetiro(id, +this.retiroMonto, this.retiroMedio, this.retiroDestino)
      .subscribe({
        next: () => {
          this.procesando.set(false);
          this.mensaje.set('Solicitud de retiro enviada.');
          this.retiroDestino = '';
          this.cargar();
        },
        error: (e) => {
          this.procesando.set(false);
          this.error.set(mensajeError(e));
        },
      });
  }
  cerrarSesion() {
    void this.auth.logout();
  }
}
