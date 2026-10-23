import { Component, input } from '@angular/core';

/**
 * Marca de KIDO: una «k» de trazo grueso cuyo brazo superior apunta hacia arriba
 * y termina en una chispa (la idea que se enciende al aprender).
 * La misma geometría se usa en favicon.svg y en el tema de Keycloak.
 */
@Component({
  selector: 'app-logo',
  standalone: true,
  host: { class: 'kido-logo', '[class.on-dark]': 'sobreOscuro()' },
  template: `
    <svg
      class="mark"
      viewBox="0 0 40 40"
      [attr.width]="tamano()"
      [attr.height]="tamano()"
      aria-hidden="true"
    >
      <rect width="40" height="40" rx="12" class="tile" />
      <path d="M14 10.5v19" class="stroke" />
      <path d="M14 21.6 23.4 13.4" class="stroke" />
      <path d="M18.6 18.4 25.6 29.5" class="stroke" />
      <circle cx="28" cy="10.6" r="3.3" class="spark" />
    </svg>
    @if (conTexto()) {
      <span class="word" [style.font-size.px]="tamano() * 0.82">kido</span>
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 9px;
      line-height: 1;
      color: var(--ink);
    }
    .mark {
      flex: none;
      display: block;
    }
    .tile {
      fill: var(--accent);
    }
    .stroke {
      fill: none;
      stroke: var(--accent-ink);
      stroke-width: 4.6;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
    .spark {
      fill: #ffc463;
    }
    .word {
      font-family: var(--font-display);
      font-weight: 700;
      letter-spacing: -0.05em;
      transform: translateY(-1px);
    }
    :host(.on-dark) {
      color: #f3f4ff;
    }
    :host(.on-dark) .tile {
      fill: #5b6dff;
    }
    :host(.on-dark) .stroke {
      stroke: #fff;
    }
  `,
})
export class Logo {
  readonly tamano = input(30);
  readonly conTexto = input(true);
  readonly sobreOscuro = input(false);
}
