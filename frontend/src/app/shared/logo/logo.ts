import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Marca oficial de KIDO. El archivo raster es la fuente única compartida por
 * navegación, pie de página y pantallas de acceso.
 */
@Component({
  selector: 'app-logo',
  standalone: true,
  host: {
    class: 'kido-logo',
    '[class.on-dark]': 'sobreOscuro()',
    '[class.on-light]': 'sobreClaro()',
    '[style.height.px]': 'tamano()',
  },
  template: `
    <img
      src="/brand/kido-mark.png?v=1"
      alt=""
    />
    <span class="word" [style.font-size.px]="tamano() * 0.94">KIDO</span>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: clamp(8px, 0.6em, 12px);
      line-height: 1;
      color: #111318;
      vertical-align: middle;
      flex: none;
    }
    img {
      display: block;
      width: auto;
      height: 100%;
      max-width: none;
      aspect-ratio: 1128 / 1079;
      object-fit: contain;
    }
    .word {
      display: block;
      font-family: var(--font-display);
      font-weight: 750;
      letter-spacing: 0.035em;
      line-height: 1;
      color: #111318;
    }
    :host(.on-dark) {
      color: #f7f8fa;
    }
    :host-context([data-theme='dark']):not(.on-dark) {
      color: #f7f8fa;
    }
    :host(.on-dark) .word,
    :host-context([data-theme='dark']):not(.on-dark) .word {
      color: #f7f8fa;
    }
    :host(.on-dark) img,
    :host-context([data-theme='dark']):not(.on-dark) img {
      box-sizing: border-box;
      padding: 2px;
      border-radius: 22%;
      background: #f7f8fa;
    }
    :host(.on-light),
    :host-context([data-theme='dark']).on-light {
      color: #111318;
    }
    :host(.on-light) .word,
    :host-context([data-theme='dark']).on-light .word {
      color: #111318;
    }
    :host(.on-light) img,
    :host-context([data-theme='dark']).on-light img {
      padding: 0;
      background: transparent;
    }
  `,
})
export class Logo {
  readonly tamano = input(30);
  readonly sobreOscuro = input(false);
  readonly sobreClaro = input(false);
}
