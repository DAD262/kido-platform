
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Auth } from '../../core/services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login implements OnInit {

  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  ngOnInit(): void {
    if (this.auth.autenticado) {
      const requested = this.route.snapshot.queryParamMap.get('returnUrl');
      const destination = requested && /^\/cursos\/\d+$/.test(requested) ? requested : this.auth.obtenerPanel();
      void this.router.navigateByUrl(destination);
    }
  }

  iniciarSesion(): void {
    void this.auth.login();
  }
}
