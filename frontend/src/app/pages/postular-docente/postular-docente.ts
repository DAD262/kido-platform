import { Component, computed, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { DatosDocente, SolicitudDocente } from '../../core/models/api.models';
import { KIDO_PIPES } from '../../core/pipes/estado.pipe';
import { AccountContext } from '../../core/services/account-context';
import { Api, camposError, mensajeError } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import {
  docenteVacio,
  Errores,
  etiquetaGrado,
  normalizarDocente,
  validarPerfilDocente,
  validarVerificacion,
} from '../../core/utils/registro';
import { DocenteForm } from '../../shared/docente-form/docente-form';
import { Icon } from '../../shared/icon/icon';

/**
 * Postulación docente para quien ya tiene cuenta (p. ej. un estudiante que quiere enseñar)
 * y seguimiento del estado: pendiente, observada (corregir y reenviar), rechazada o aprobada.
 */
@Component({
  selector: 'app-postular-docente',
  standalone: true,
  imports: [FormsModule, RouterLink, Icon, DocenteForm, KIDO_PIPES],
  templateUrl: './postular-docente.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './postular-docente.scss',
})
export class PostularDocente implements OnInit {
  readonly auth = inject(Auth);
  readonly account = inject(AccountContext);
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  readonly editando = signal(false);
  readonly enviando = signal(false);
  readonly activando = signal(false);
  readonly errores = signal<Errores>({});
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly documento = signal<File | null>(null);
  datos: DatosDocente = docenteVacio();
  readonly grado = etiquetaGrado;

  readonly solicitud = computed(() => this.account.perfil()?.solicitudDocente ?? null);
  /** Métodos (no computed): los roles viven en el token de Keycloak, que no es una señal. */
  esDocente(): boolean {
    return this.auth.tieneRol('DOCENTE');
  }
  /** Aprobada en el backend, pero el token aún no trae el rol: hay que renovarlo. */
  faltaActivar(): boolean {
    return (
      !this.esDocente() &&
      (this.solicitud()?.estado === 'APROBADA' || !!this.account.perfil()?.docenteVerificado)
    );
  }
  mostrarFormulario(): boolean {
    const s = this.solicitud();
    return !this.esDocente() && !this.faltaActivar() && (!s || this.editando());
  }

  ngOnInit(): void {
    void this.account.asegurar(true);
  }

  corregir(s: SolicitudDocente): void {
    this.datos = {
      dni: s.dni,
      telefono: s.telefono,
      gradoAcademico: s.gradoAcademico,
      especialidad: s.especialidad,
      institucion: s.institucion,
      aniosExperiencia: s.aniosExperiencia,
      enlaceProfesional: s.enlaceProfesional ?? '',
      presentacion: s.presentacion,
      declaracionAceptada: false,
    };
    this.documento.set(null);
    this.errores.set({});
    this.editando.set(true);
  }

  cancelar(): void {
    this.editando.set(false);
    this.errores.set({});
  }

  enviar(): void {
    if (this.enviando()) return;
    const requiereDoc = !this.solicitud()?.documento;
    const errores = {
      ...validarPerfilDocente(this.datos),
      ...validarVerificacion(this.datos, this.documento(), requiereDoc),
    };
    this.errores.set(errores);
    if (Object.keys(errores).length) {
      this.error.set('Revisa los campos marcados.');
      return;
    }
    this.enviando.set(true);
    this.error.set('');
    this.api.postularDocente(normalizarDocente(this.datos), this.documento()).subscribe({
      next: () => {
        this.enviando.set(false);
        this.editando.set(false);
        this.mensaje.set('Postulación enviada. Administración la revisará pronto.');
        void this.account.asegurar(true);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (e) => {
        this.enviando.set(false);
        this.errores.set(camposError(e));
        this.error.set(mensajeError(e));
      },
    });
  }

  limpiarError(campo: string): void {
    if (!this.errores()[campo]) return;
    this.errores.update((v) => {
      const resto = { ...v };
      delete resto[campo];
      return resto;
    });
  }

  /** Renueva el token para que traiga el rol DOCENTE recién asignado. */
  async activar(): Promise<void> {
    this.activando.set(true);
    const ok = await this.auth.refrescarSesion();
    this.activando.set(false);
    if (ok && this.auth.tieneRol('DOCENTE')) {
      void this.router.navigateByUrl('/docente');
      return;
    }
    // Si la sesión no se pudo renovar, un nuevo inicio de sesión trae los roles actualizados.
    void this.auth.login('/docente');
  }

  verDocumento(): void {
    const ventana = window.open('', '_blank');
    this.api.miDocumentoDocente().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        if (ventana) ventana.location.href = url;
        else window.open(url, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (e) => {
        ventana?.close();
        this.error.set(mensajeError(e));
      },
    });
  }
}
