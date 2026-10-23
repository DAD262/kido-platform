import { Injectable, signal } from '@angular/core';

export type Apariencia = 'light' | 'dark' | 'system';

/** Lectura segura de localStorage (puede fallar en modo privado). */
function leer(clave: string): string | null {
  try {
    return localStorage.getItem(clave);
  } catch {
    return null;
  }
}
function guardar(clave: string, valor: string): void {
  try {
    localStorage.setItem(clave, valor);
  } catch {
    /* sin persistencia: la preferencia vale solo para esta sesión */
  }
}

@Injectable({ providedIn: 'root' })
export class VisualPreferences {
  private readonly media = window.matchMedia('(prefers-color-scheme: dark)');
  readonly apariencia = signal<Apariencia>(this.leerApariencia());
  readonly compacto = signal(leer('kido-compact') === 'true');
  readonly movimientoReducido = signal(leer('kido-reduced-motion') === 'true');
  /** Tema efectivo (resuelve «automático»). Es una señal para que la UI reaccione. */
  readonly oscuro = signal(false);

  constructor() {
    this.aplicar();
    this.media.addEventListener('change', () => {
      if (this.apariencia() === 'system') this.aplicar();
    });
  }

  setApariencia(valor: Apariencia): void {
    this.apariencia.set(valor);
    guardar('kido-theme', valor);
    this.aplicar();
  }

  setCompacto(valor: boolean): void {
    this.compacto.set(valor);
    guardar('kido-compact', String(valor));
    this.aplicar();
  }

  setMovimientoReducido(valor: boolean): void {
    this.movimientoReducido.set(valor);
    guardar('kido-reduced-motion', String(valor));
    this.aplicar();
  }

  /** Compatibilidad con el código anterior. */
  get esOscuro(): boolean {
    return this.oscuro();
  }

  alternarTema(): void {
    this.setApariencia(this.oscuro() ? 'light' : 'dark');
  }

  private leerApariencia(): Apariencia {
    const v = leer('kido-theme');
    return v === 'light' || v === 'dark' || v === 'system' ? v : 'system';
  }

  private aplicar(): void {
    const oscuro =
      this.apariencia() === 'dark' || (this.apariencia() === 'system' && this.media.matches);
    this.oscuro.set(oscuro);
    const root = document.documentElement;
    root.setAttribute('data-theme', oscuro ? 'dark' : 'light');
    root.setAttribute('data-density', this.compacto() ? 'compact' : 'comfortable');
    root.setAttribute('data-motion', this.movimientoReducido() ? 'reduced' : 'full');
  }
}
