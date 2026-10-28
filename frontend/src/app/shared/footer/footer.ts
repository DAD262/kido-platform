import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../../core/services/api';
import { Logo } from '../logo/logo';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink, Logo],
  templateUrl: './footer.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './footer.scss',
})
export class Footer implements OnInit {
  private readonly api=inject(Api);
  readonly reclamaciones=signal('');
  ngOnInit(){this.api.informacionComercial().subscribe({next:d=>{if(d.lista)this.reclamaciones.set(d.datos.reclamacionesUrl);},error:()=>{}});}

  readonly anio = new Date().getFullYear();
}
