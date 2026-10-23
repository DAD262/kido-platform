import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Perfil } from '../models/api.models';
import { Api, mensajeError } from './api';
import { Auth } from './auth';

type Estado = 'inactivo' | 'cargando' | 'listo' | 'error';

/**
 * Perfil KIDO de la sesión actual (GET /api/v1/usuarios/me).
 * Su id numérico es el estudianteId/docenteId que usan los demás microservicios,
 * así que ya no hay que «vincular» un número a mano.
 *
 * Si kido-usuario-ms no responde, se permite un número manual SOLO como modo demo
 * (queda en localStorage y no verifica titularidad).
 */
@Injectable({ providedIn: 'root' })
export class AccountContext {
  private readonly api = inject(Api);
  private readonly auth = inject(Auth);

  readonly perfil = signal<Perfil | null>(null);
  readonly estado = signal<Estado>('inactivo');
  readonly error = signal('');
  private readonly manual = signal<number | null>(null);
  private pendiente: Promise<Perfil | null> | null = null;

  /** El número a usar en las llamadas; null mientras no se conozca. */
  readonly id = computed(() => this.perfil()?.id ?? this.manual());
  readonly esManual = computed(() => !this.perfil() && this.manual() !== null);

  constructor() {
    const guardado = Number(localStorage.getItem(this.claveManual()));
    if (Number.isSafeInteger(guardado) && guardado > 0) this.manual.set(guardado);
  }

  /** Carga el perfil una vez por sesión; las llamadas simultáneas comparten la misma petición. */
  asegurar(forzar = false): Promise<Perfil | null> {
    if (!this.auth.autenticado) return Promise.resolve(null);
    if (!forzar && this.estado() === 'listo') return Promise.resolve(this.perfil());
    if (this.pendiente) return this.pendiente;
    this.estado.set('cargando');
    this.error.set('');
    this.pendiente = firstValueFrom(this.api.me())
      .then((perfil) => {
        this.perfil.set(perfil);
        this.estado.set('listo');
        return perfil;
      })
      .catch((e) => {
        this.estado.set('error');
        this.error.set(mensajeError(e));
        return null;
      })
      .finally(() => (this.pendiente = null));
    return this.pendiente;
  }

  /** Modo demo cuando el servicio de cuentas no está disponible. */
  setId(value: number) {
    if (!Number.isSafeInteger(value) || value <= 0) return false;
    this.manual.set(value);
    localStorage.setItem(this.claveManual(), String(value));
    return true;
  }

  clear() {
    this.manual.set(null);
    localStorage.removeItem(this.claveManual());
  }

  private claveManual() {
    return `kido-demo-numeric-id:${this.auth.subject}`;
  }
}
