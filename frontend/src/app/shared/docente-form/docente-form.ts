import { Component, input, model, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatosDocente } from '../../core/models/api.models';
import { Errores, GRADOS, pesoLegible, revisarPdf, soloDigitos } from '../../core/utils/registro';
import { Icon } from '../icon/icon';

/**
 * Formulario de verificación docente, compartido por /registro (pasos 2 y 3)
 * y por /postular-docente. El objeto `datos` se edita en el lugar.
 */
@Component({
  selector: 'app-docente-form',
  standalone: true,
  imports: [FormsModule, Icon],
  templateUrl: './docente-form.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './docente-form.scss',
})
export class DocenteForm {
  readonly dniFijo = input(false);
  readonly datos = input.required<DatosDocente>();
  readonly errores = input<Errores>({});
  readonly seccion = input<'perfil' | 'verificacion'>('perfil');
  /** Nombre del PDF ya enviado (al corregir una solicitud observada). */
  readonly documentoActual = input<string | null>(null);
  readonly documento = model<File | null>(null);
  readonly editado = output<string>();

  readonly grados = GRADOS;
  readonly arrastrando = signal(false);
  readonly errorArchivo = signal('');
  readonly peso = pesoLegible;

  /** Alias para el template: los [(ngModel)] escriben directamente en el objeto. */
  get d(): DatosDocente {
    return this.datos();
  }

  cambio(campo: string) {
    this.editado.emit(campo);
  }

  dni(valor: string) {
    this.datos().dni = soloDigitos(valor, 8);
    this.cambio('dni');
  }

  largoPresentacion() {
    return (this.datos().presentacion ?? '').trim().length;
  }

  async elegir(archivos: FileList | null | undefined) {
    const archivo = archivos?.item(0);
    if (!archivo) return;
    const problema = await revisarPdf(archivo);
    this.errorArchivo.set(problema ?? '');
    this.documento.set(problema ? null : archivo);
    this.cambio('documento');
  }

  soltar(evento: DragEvent) {
    evento.preventDefault();
    this.arrastrando.set(false);
    void this.elegir(evento.dataTransfer?.files);
  }

  quitar() {
    this.documento.set(null);
    this.errorArchivo.set('');
  }

  error(campo: string) {
    return campo === 'documento'
      ? this.errorArchivo() || this.errores()[campo]
      : this.errores()[campo];
  }
}
