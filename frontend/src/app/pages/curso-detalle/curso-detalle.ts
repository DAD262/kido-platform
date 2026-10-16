import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Api, mensajeError } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { AccountContext } from '../../core/services/account-context';
import { ContenidoCurso, Curso } from '../../core/models/api.models';
@Component({selector:'app-curso-detalle',standalone:true,imports:[RouterLink],templateUrl:'./curso-detalle.html',styleUrl:'./curso-detalle.scss'})
export class CursoDetalle implements OnInit {
  private readonly api=inject(Api);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);
  readonly auth=inject(Auth);readonly account=inject(AccountContext);
  readonly curso=signal<Curso|null>(null);readonly contenido=signal<ContenidoCurso|null>(null);
  readonly cargando=signal(true);readonly enviando=signal(false);readonly error=signal('');readonly mensaje=signal('');
  ngOnInit(){const id=Number(this.route.snapshot.paramMap.get('id'));
    if(!Number.isSafeInteger(id)||id<=0){this.error.set('Identificador de curso inválido.');this.cargando.set(false);return}
    forkJoin({curso:this.api.curso(id),contenido:this.api.contenido(id)}).subscribe({next:v=>{this.curso.set(v.curso);this.contenido.set(v.contenido);this.cargando.set(false)},error:e=>{this.error.set(mensajeError(e));this.cargando.set(false)}})
  }
  inscribirme(){const curso=this.curso(),contenido=this.contenido();if(!curso||!contenido)return;
    if(!this.auth.autenticado){void this.router.navigate(['/login'],{queryParams:{returnUrl:'/cursos/'+curso.id}});return}
    if(!this.auth.tieneRol('ESTUDIANTE')){this.error.set('Para inscribirte debes ingresar con una cuenta de estudiante.');return}
    const estudianteId=this.account.id();if(!estudianteId){this.error.set('Configura tu ID numérico de estudiante en Mi espacio antes de inscribirte.');return}
    if(curso.estado!=='PUBLICADO'){this.error.set('Solo puedes inscribirte en cursos publicados.');return}
    this.error.set('');this.mensaje.set('');this.enviando.set(true);
    if(curso.tipo==='GRATUITO'){
      const ids=contenido.modulos.flatMap(m=>m.lecciones.map(l=>l.id));
      this.api.crearInscripcion(estudianteId,curso.id,ids,contenido.configuracion).subscribe({next:()=>{this.enviando.set(false);this.mensaje.set('¡Inscripción creada! Ve a tu panel para comenzar.')},error:e=>{this.enviando.set(false);this.error.set(mensajeError(e))}});
    }else{
      this.api.crearOrden(estudianteId,curso.id).subscribe({next:orden=>{
        this.api.iniciarPago(orden.id).subscribe({next:p=>{this.enviando.set(false);const url=p.sandboxInitPoint || p.initPoint;
          if(url&&/^https:\/\//i.test(url)){this.mensaje.set('Orden creada. Se abrirá la pasarela de pago de prueba.');window.open(url,'_blank','noopener,noreferrer')}
          else this.mensaje.set(`Orden #${orden.id} creada. Mercado Pago no devolvió una URL; revisa su configuración sandbox.`);
        },error:e=>{this.enviando.set(false);this.error.set(`Orden #${orden.id} creada, pero no pudo iniciarse Mercado Pago: ${mensajeError(e)}`)}})
      },error:e=>{this.enviando.set(false);this.error.set(mensajeError(e))}})
    }
  }
}
