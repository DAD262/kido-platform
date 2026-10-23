import { Component, computed, ElementRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { DatosDocente, RegistroResponse, TipoCuenta } from '../../core/models/api.models';
import { Api, camposError, mensajeError } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import {
  DatosCuenta,
  docenteVacio,
  Errores,
  etiquetaGrado,
  fuerzaContrasena,
  limpiar,
  normalizarDocente,
  REGLAS_CONTRASENA,
  validarCuenta,
  validarPerfilDocente,
  validarVerificacion,
} from '../../core/utils/registro';
import { AuthShell, ModoAcceso } from '../../shared/auth-shell/auth-shell';
import { DocenteForm } from '../../shared/docente-form/docente-form';
import { Icon } from '../../shared/icon/icon';

type Paso = 'tipo' | 'cuenta' | 'perfil' | 'verificacion' | 'listo';

const PASOS_DOCENTE: { id: Paso; etiqueta: string }[] = [
  { id: 'cuenta', etiqueta: 'Tu cuenta' },
  { id: 'perfil', etiqueta: 'Perfil profesional' },
  { id: 'verificacion', etiqueta: 'Verificación' },
];
const CAMPOS_CUENTA = ['nombres', 'apellidos', 'email', 'password', 'aceptaTerminos', 'tipo'];
const CAMPOS_PERFIL = [
  'dni',
  'telefono',
  'gradoAcademico',
  'especialidad',
  'institucion',
  'aniosExperiencia',
  'enlaceProfesional',
  'presentacion',
];

/**
 * Registro en KIDO. Estudiante: un paso. Docente: tres pasos; la cuenta nace con rol
 * ESTUDIANTE y una solicitud PENDIENTE que administración aprueba (rol DOCENTE),
 * observa o rechaza. El rol nunca se elige libremente desde el navegador.
 */
@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [FormsModule, RouterLink, Icon, AuthShell, DocenteForm],
  templateUrl: './registro.html',
  styleUrl: './registro.scss',
})
export class Registro implements OnInit {
  readonly auth = inject(Auth);
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly tipo = signal<TipoCuenta | null>(null);
  readonly paso = signal<Paso>('tipo');
  readonly errores = signal<Errores>({});
  readonly errorGeneral = signal('');
  readonly enviando = signal(false);
  readonly verPassword = signal(false);
  readonly emailEstado = signal<'idle' | 'revisando' | 'libre' | 'ocupado'>('idle');
  readonly resultado = signal<RegistroResponse | null>(null);
  readonly documento = signal<File | null>(null);
  /** Para que el medidor reaccione mientras se escribe. */
  readonly password = signal('');

  readonly cuenta: DatosCuenta = {
    nombres: '',
    apellidos: '',
    email: '',
    password: '',
    aceptaTerminos: false,
  };
  readonly docente: DatosDocente = docenteVacio();

  readonly reglas = REGLAS_CONTRASENA;
  readonly grado = etiquetaGrado;
  readonly fuerza = computed(() => fuerzaContrasena(this.password()));
  readonly pasos = computed(() => (this.tipo() === 'DOCENTE' ? PASOS_DOCENTE : []));
  readonly indice = computed(() => this.pasos().findIndex((p) => p.id === this.paso()));
  readonly modoShell = computed<ModoAcceso>(() =>
    this.tipo() === 'DOCENTE'
      ? 'docente'
      : this.tipo() === 'ESTUDIANTE'
        ? 'estudiante'
        : 'ingresar',
  );
  private ultimoEmailRevisado = '';

  ngOnInit(): void {
    const t = this.route.snapshot.queryParamMap.get('tipo');
    if (t === 'docente') this.elegir('DOCENTE');
    else if (t === 'estudiante') this.elegir('ESTUDIANTE');
  }

  elegir(tipo: TipoCuenta): void {
    this.tipo.set(tipo);
    this.paso.set('cuenta');
    this.errorGeneral.set('');
    this.arriba();
  }

  cambiarTipo(): void {
    this.paso.set('tipo');
    this.errores.set({});
    this.errorGeneral.set('');
  }

  atras(): void {
    const i = this.indice();
    if (i > 0) this.irA(this.pasos()[i - 1].id);
    else this.cambiarTipo();
  }

  irA(paso: Paso): void {
    this.paso.set(paso);
    this.errorGeneral.set('');
    this.arriba();
  }

  /** Valida el paso actual; si está bien avanza o, en el último, envía. */
  siguiente(): void {
    if (this.enviando()) return;
    const paso = this.paso();
    const errores =
      paso === 'cuenta'
        ? validarCuenta(this.cuenta)
        : paso === 'perfil'
          ? validarPerfilDocente(this.docente)
          : validarVerificacion(this.docente, this.documento(), true);
    if (paso === 'cuenta' && this.emailEstado() === 'ocupado' && !errores['email'])
      errores['email'] = 'Ese correo ya tiene una cuenta. Ingresa o usa otro.';
    this.errores.set(errores);
    if (Object.keys(errores).length) {
      this.enfocarError();
      return;
    }
    const esUltimo = this.tipo() === 'ESTUDIANTE' || paso === 'verificacion';
    if (esUltimo) this.enviar();
    else this.irA(paso === 'cuenta' ? 'perfil' : 'verificacion');
  }

  private enviar(): void {
    const tipo = this.tipo();
    if (!tipo) return;
    this.enviando.set(true);
    this.errorGeneral.set('');
    this.api
      .registrar(
        {
          tipo,
          nombres: limpiar(this.cuenta.nombres),
          apellidos: limpiar(this.cuenta.apellidos),
          email: limpiar(this.cuenta.email).toLowerCase(),
          password: this.cuenta.password,
          aceptaTerminos: this.cuenta.aceptaTerminos,
          docente: tipo === 'DOCENTE' ? normalizarDocente(this.docente) : null,
        },
        tipo === 'DOCENTE' ? this.documento() : null,
      )
      .subscribe({
        next: (r) => {
          this.enviando.set(false);
          this.cuenta.password = '';
          this.password.set('');
          this.resultado.set(r);
          this.paso.set('listo');
          this.arriba();
        },
        error: (e: unknown) => {
          this.enviando.set(false);
          this.manejarError(e);
        },
      });
  }

  private manejarError(e: unknown): void {
    const campos = camposError(e);
    if (e instanceof HttpErrorResponse && e.status === 409 && !Object.keys(campos).length) {
      if (/dni/i.test(mensajeError(e))) campos['dni'] = mensajeError(e);
      else {
        campos['email'] = mensajeError(e);
        this.emailEstado.set('ocupado');
      }
    }
    const claves = Object.keys(campos);
    if (claves.length) {
      this.errores.set(campos);
      const destino: Paso = claves.some((c) => CAMPOS_CUENTA.includes(c))
        ? 'cuenta'
        : claves.some((c) => CAMPOS_PERFIL.includes(c))
          ? 'perfil'
          : this.paso();
      if (destino !== this.paso()) this.paso.set(destino);
      this.enfocarError();
      return;
    }
    this.errorGeneral.set(mensajeError(e));
  }

  /** Comprueba el correo al salir del campo, sin bloquear el formulario si falla. */
  revisarEmail(): void {
    const email = limpiar(this.cuenta.email).toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email) || email === this.ultimoEmailRevisado) return;
    this.ultimoEmailRevisado = email;
    this.emailEstado.set('revisando');
    this.api.emailDisponible(email).subscribe({
      next: (r) => {
        if (limpiar(this.cuenta.email).toLowerCase() !== email) return;
        this.emailEstado.set(r.disponible ? 'libre' : 'ocupado');
        if (!r.disponible)
          this.errores.update((v) => ({
            ...v,
            email: 'Ese correo ya tiene una cuenta. Ingresa o usa otro.',
          }));
      },
      error: () => this.emailEstado.set('idle'),
    });
  }

  cambioEmail(): void {
    this.emailEstado.set('idle');
    this.ultimoEmailRevisado = '';
    this.limpiarError('email');
  }

  cambioPassword(valor: string): void {
    this.password.set(valor);
    this.limpiarError('password');
  }

  limpiarError(campo: string): void {
    if (!this.errores()[campo]) return;
    this.errores.update((v) => {
      const resto = { ...v };
      delete resto[campo];
      return resto;
    });
  }

  cumple(regla: (typeof REGLAS_CONTRASENA)[number]): boolean {
    return regla.cumple(this.password());
  }

  ingresar(): void {
    void this.auth.login(undefined, this.resultado()?.usuario);
  }

  nombreCompleto(): string {
    return `${limpiar(this.cuenta.nombres)} ${limpiar(this.cuenta.apellidos)}`.trim();
  }

  private arriba(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private enfocarError(): void {
    setTimeout(() => {
      const el = (this.host.nativeElement as HTMLElement).querySelector<HTMLElement>(
        '.invalid input:not([type=radio]), .invalid textarea, .invalid label.drop, input[aria-invalid="true"]',
      );
      el?.focus();
      el?.scrollIntoView({ block: 'center', behavior: 'smooth' });
    });
  }
}
