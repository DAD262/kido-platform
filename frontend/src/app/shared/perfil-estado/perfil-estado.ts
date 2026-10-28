import { Component, inject, input, output, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AccountContext } from '../../core/services/account-context';
import { Icon } from '../icon/icon';

/**
 * Estado del perfil KIDO en los paneles. Si todo va bien no muestra nada:
 * el número de estudiante/docente se resuelve solo con GET /usuarios/me.
 * Solo aparece si el servicio de cuentas falla, con reintento y un modo demo manual.
 */
@Component({
  selector: 'app-perfil-estado',
  standalone: true,
  imports: [FormsModule, Icon],
  template: `
    @if (account.estado() === 'cargando' && !account.id()) {
      <p class="muted loading" role="status"><span class="spinner"></span>Preparando tu panel…</p>
    } @else if (account.estado() === 'error' && !account.esManual()) {
      <div class="alert error" role="alert">
        <span> <strong>No pudimos cargar tu perfil.</strong> {{ account.error() }} </span>
        <button class="btn btn-small btn-outline" type="button" (click)="reintentar()">
          <app-icon name="refresh" [size]="14" />Reintentar
        </button>
      </div>
      <details class="manual">
        <summary>¿Estás probando sin el servicio de cuentas? Usa un número manual</summary>
        <form (ngSubmit)="usarManual()">
          <label class="field">
            Número de {{ rol() }}
            <input
              name="manual"
              type="number"
              min="1"
              step="1"
              [(ngModel)]="manual"
              placeholder="Ej. {{ ejemplo() }}"
            />
          </label>
          <button class="btn btn-outline" type="submit">Usar</button>
          <p class="note">Solo para demostraciones: no verifica que el número sea tuyo.</p>
        </form>
      </details>
    } @else if (account.esManual()) {
      <div class="alert warn">
        <span>Modo demo: usando el {{ rol() }} n.º {{ account.id() }} sin verificar.</span>
        <button class="btn btn-small btn-outline" type="button" (click)="salirManual()">
          Volver a mi perfil
        </button>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: `
    :host {
      display: block;
    }
    .loading {
      margin: 8px 0 16px;
      font-size: 14px;
    }
    .manual {
      margin: -4px 0 16px;
      font-size: 14px;
    }
    .manual summary {
      cursor: pointer;
      color: var(--muted);
    }
    .manual form {
      display: flex;
      align-items: flex-end;
      flex-wrap: wrap;
      gap: 10px;
      margin-top: 10px;
    }
    .manual .field input {
      width: 160px;
    }
    .manual .note {
      flex-basis: 100%;
    }
  `,
})
export class PerfilEstado {
  readonly account = inject(AccountContext);
  readonly rol = input<'estudiante' | 'docente'>('estudiante');
  /** Se emite cuando ya hay un número utilizable. */
  readonly listo = output<number>();
  manual: number | null = null;

  ejemplo() {
    return this.rol() === 'docente' ? 1 : 1001;
  }

  async reintentar() {
    await this.account.asegurar(true);
    const id = this.account.id();
    if (id) this.listo.emit(id);
  }

  usarManual() {
    if (this.manual && this.account.setId(Number(this.manual))) this.listo.emit(this.account.id()!);
  }

  async salirManual() {
    this.account.clear();
    await this.reintentar();
  }
}
