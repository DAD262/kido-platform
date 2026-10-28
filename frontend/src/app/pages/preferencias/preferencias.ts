import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Apariencia, VisualPreferences } from '../../core/services/visual-preferences';
import { Auth } from '../../core/services/auth';

@Component({
  selector: 'app-preferencias',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './preferencias.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './preferencias.scss',
})
export class Preferencias {
  readonly visual = inject(VisualPreferences);
  readonly auth = inject(Auth);
  readonly temas: { valor: Apariencia; texto: string }[] = [
    { valor: 'light', texto: 'Claro' },
    { valor: 'dark', texto: 'Oscuro' },
    { valor: 'system', texto: 'Como mi sistema' },
  ];
}
