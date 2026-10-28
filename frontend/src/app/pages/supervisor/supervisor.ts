import {Component,inject,OnInit,signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {finalize} from 'rxjs';
import {Auth} from '../../core/services/auth';
import {mensajeError} from '../../core/services/api';
import {ContenidoCurso} from '../../core/models/api.models';
import {Icon} from '../../shared/icon/icon';
import {KIDO_PIPES} from '../../core/pipes/estado.pipe';
interface CursoSupervisado {id:number;titulo:string;docenteId:number;docente:string;estado:string}
interface Seguimiento {curso:CursoSupervisado;estudiantes:{id:number;nombre:string;estado:string;progreso:number}[];contenido:ContenidoCurso}
@Component({selector:'app-supervisor',standalone:true,imports:[Icon,KIDO_PIPES],templateUrl:'./supervisor.html',styleUrl:'./supervisor.scss'})
export class Supervisor implements OnInit {
 readonly auth=inject(Auth);private readonly http=inject(HttpClient);private readonly root='/api/v1/usuarios/supervision/cursos';
 readonly cursos=signal<CursoSupervisado[]>([]);readonly cargando=signal(false);readonly cargandoDetalle=signal(false);
 readonly error=signal('');readonly errorDetalle=signal('');readonly detalle=signal<Seguimiento|null>(null);readonly seleccionado=signal<number|null>(null);
 private lectura=0;
 ngOnInit(){this.cargar();}
 cargar(){this.cargando.set(true);this.error.set('');this.http.get<CursoSupervisado[]>(this.root).pipe(finalize(()=>this.cargando.set(false))).subscribe({next:r=>this.cursos.set(r),error:e=>this.error.set(mensajeError(e))});}
 abrir(c:CursoSupervisado){const version=++this.lectura;this.seleccionado.set(c.id);this.detalle.set(null);this.errorDetalle.set('');this.cargandoDetalle.set(true);
 this.http.get<Seguimiento>(this.root+'/'+c.id).subscribe({next:r=>{if(version===this.lectura){this.detalle.set(r);this.cargandoDetalle.set(false);}},error:e=>{if(version===this.lectura){this.errorDetalle.set(mensajeError(e));this.cargandoDetalle.set(false);}}});}
 promedio(d:Seguimiento){return d.estudiantes.length?Math.round(d.estudiantes.reduce((s,e)=>s+e.progreso,0)/d.estudiantes.length):0;}
 enlace(url:string){return /^https?:\/\//i.test(url)?url:null;}
}
