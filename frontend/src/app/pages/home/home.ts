import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, mensajeError } from '../../core/services/api';
import { Curso } from '../../core/models/api.models';
import { CursoCard } from '../../shared/curso-card/curso-card';
@Component({selector:'app-home',standalone:true,imports:[RouterLink,CursoCard],templateUrl:'./home.html',styleUrl:'./home.scss'})
export class Home implements OnInit {
 private readonly api=inject(Api);
 readonly cursos=signal<Curso[]>([]); readonly cargando=signal(true); readonly error=signal('');
 ngOnInit(){this.api.publicados().subscribe({next:c=>{this.cursos.set(c);this.cargando.set(false)},error:e=>{this.error.set(mensajeError(e));this.cargando.set(false)}})}
}
