import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../core/services/auth';
@Component({ selector: 'app-navbar', standalone: true, imports:[RouterLink,RouterLinkActive], templateUrl:'./navbar.html', styleUrl:'./navbar.scss' })
export class Navbar {
  readonly auth = inject(Auth);
  readonly login = () => void this.auth.login();
  readonly logout = () => void this.auth.logout();
}
