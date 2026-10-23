import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Navbar } from './shared/navbar/navbar';
import { Footer } from './shared/footer/footer';
import { CommandPalette } from './shared/command-palette/command-palette';
import { VisualPreferences } from './core/services/visual-preferences';
import { Auth } from './core/services/auth';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Navbar, Footer, CommandPalette],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);
  // Se inyecta para aplicar el tema guardado desde el arranque.
  private readonly visual = inject(VisualPreferences);
  readonly auth = inject(Auth);
  readonly chromeless = signal(false);

  constructor() {
    this.actualizarShell(this.router.url);
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => this.actualizarShell(event.urlAfterRedirects));
  }

  private actualizarShell(url: string): void {
    this.chromeless.set(/^\/(login|registro)(\?|$)/.test(url));
  }
}
