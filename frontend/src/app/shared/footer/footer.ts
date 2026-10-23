import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Logo } from '../logo/logo';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink, Logo],
  templateUrl: './footer.html',
  styleUrl: './footer.scss',
})
export class Footer {
  readonly anio = new Date().getFullYear();
}
