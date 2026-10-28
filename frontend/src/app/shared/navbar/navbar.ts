import { Component, ElementRef, HostListener, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../core/services/auth';
import { VisualPreferences } from '../../core/services/visual-preferences';
import { CommandPaletteService } from '../command-palette/command-palette';
import { Icon } from '../icon/icon';
import { Logo } from '../logo/logo';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, Icon, Logo],
  templateUrl: './navbar.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './navbar.scss',
})
export class Navbar {
  readonly auth = inject(Auth);
  readonly visual = inject(VisualPreferences);
  readonly palette = inject(CommandPaletteService);
  private readonly host = inject(ElementRef<HTMLElement>);
  readonly menu = signal(false);
  readonly esMac = /Mac|iPhone|iPad/.test(navigator.platform);

  login(): void {
    void this.auth.login();
  }
  logout(): void {
    void this.auth.logout();
  }
  cuenta(): void {
    this.menu.set(false);
    void this.auth.cuenta();
  }

  @HostListener('document:click', ['$event'])
  cerrarFuera(e: MouseEvent) {
    if (this.menu() && !this.host.nativeElement.contains(e.target as Node)) this.menu.set(false);
  }
  @HostListener('document:keydown.escape')
  cerrarConEscape() {
    this.menu.set(false);
  }
}
