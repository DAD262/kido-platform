import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, catchError, of, Observable } from 'rxjs';
import { Api, mensajeError } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { AccountContext } from '../../core/services/account-context';
import { Certificado, ContenidoCurso, Curso, Material, EstadoAcademico, Inscripcion, Notificacion, Orden } from '../../core/models/api.models';
@Component({selector:'app-estudiante',standalone:true,imports:[FormsModule,RouterLink],templateUrl:'./estudiante.html',styleUrl:'./estudiante.scss'})
export class Estudiante implements OnInit {
  readonly auth=inject(Auth);readonly account=inject(AccountContext);private readonly api=inject(Api);
  idInput:number|null=this.account.id(); readonly cargando=signal(false);readonly procesando=signal(false);
  readonly error=signal('');readonly mensaje=signal('');readonly inscripciones=signal<Inscripcion[]>([]);
  readonly ordenes=signal<Orden[]>([]);readonly notificaciones=signal<Notificacion[]>([]);
  readonly cursos=signal<Curso[]>([]);readonly seleccionada=signal<Inscripcion|null>(null);
  readonly contenido=signal<ContenidoCurso|null>(null);readonly estado=signal<EstadoAcademico|null>(null);
  readonly certificado=signal<Certificado|null>(null); readonly tab=signal<'cursos'|'pagos'|'avisos'>('cursos');
  readonly fallos=signal<string[]>([]);readonly cargandoContenido=signal(false);readonly reembolso=signal<Orden|null>(null);motivoReembolso='';busqueda='';readonly leccionActiva=signal<number|null>(null);private lectura=0;
  private parcial<T>(nombre:string,op:Observable<T>,vacio:T){return op.pipe(catchError(e=>{this.fallos.update(v=>[...v,`${nombre}: ${mensajeError(e)}`]);return of(vacio)}))}
  inscripcionesFiltradas(){return this.inscripciones().filter(i=>this.titulo(i.cursoId).toLowerCase().includes(this.busqueda.toLowerCase()))}
  continuar(){const l=this.contenido()?.modulos.flatMap(m=>m.lecciones).find(l=>!this.completada(l.id));if(l)this.leccionActiva.set(l.id);else this.mensaje.set('Ya completaste todas las lecciones disponibles.')}
  abrirLeccion(id:number){this.leccionActiva.set(id)}
  abrirEnlace(url:string){if(/^https?:\/\//i.test(url))window.open(url,'_blank','noopener,noreferrer');else this.error.set('El enlace de la lección debe usar http:// o https://.')}
  enviarReembolso(){const o=this.reembolso();if(!o||this.procesando()||!this.motivoReembolso.trim())return;this.procesando.set(true);this.error.set('');this.api.solicitarReembolso(o.id,this.motivoReembolso.trim()).subscribe({next:()=>{this.procesando.set(false);this.reembolso.set(null);this.mensaje.set('Solicitud enviada. Administración revisará el reembolso.')},error:e=>{this.procesando.set(false);this.error.set(mensajeError(e))}})}
  ngOnInit(){if(this.account.id())this.cargar()}
  promedioProgreso(){const list=this.inscripciones();return list.length?Math.round(list.reduce((sum,i)=>sum+i.porcentajeProgreso,0)/list.length):0}
  noLeidas(){return this.notificaciones().filter(n=>!n.leida).length}
  guardarId(){if(this.cargando()||this.procesando())return;if(!this.idInput||!this.account.setId(Number(this.idInput))){this.error.set('Introduce un ID numérico válido (mayor que cero).');return}this.cargar()}
  cargar(){const id=this.account.id();if(!id||this.cargando())return;this.cargando.set(true);this.error.set('');this.fallos.set([]);
    forkJoin({inscripciones:this.parcial('Inscripciones',this.api.inscripcionesEstudiante(id),[]),ordenes:this.parcial('Pagos',this.api.ordenesEstudiante(id),[]),notificaciones:this.parcial('Notificaciones',this.api.notificacionesUsuario(id),[]),cursos:this.parcial('Catálogo',this.api.publicados(),[])}).subscribe({next:r=>{this.inscripciones.set(r.inscripciones);this.ordenes.set(r.ordenes);this.notificaciones.set(r.notificaciones);this.cursos.set(r.cursos);this.cargando.set(false)},error:e=>{this.cargando.set(false);this.error.set(mensajeError(e))}})
  }
  titulo(cursoId:number){return this.cursos().find(c=>c.id===cursoId)?.titulo || `Curso #${cursoId}`}
  abrir(ins:Inscripcion){const version=++this.lectura;this.seleccionada.set(ins);this.contenido.set(null);this.estado.set(null);this.certificado.set(null);this.leccionActiva.set(null);this.error.set('');this.cargandoContenido.set(true);
    this.api.contenido(ins.cursoId).subscribe({next:r=>{if(version!==this.lectura)return;this.contenido.set(r);this.cargandoContenido.set(false);this.continuar()},error:e=>{if(version!==this.lectura)return;this.cargandoContenido.set(false);this.error.set(mensajeError(e))}});
    this.api.estadoAcademico(ins.id).subscribe({next:r=>{if(version===this.lectura)this.estado.set(r)},error:e=>{if(version===this.lectura)this.error.set(mensajeError(e))}});
    this.api.certificado(ins.id).subscribe({next:r=>{if(version===this.lectura)this.certificado.set(r)},error:()=>{if(version===this.lectura)this.certificado.set(null)}});
  }
  completar(leccionId:number){const ins=this.seleccionada();if(!ins||this.procesando())return;this.procesando.set(true);this.error.set('');
    this.api.completarLeccion(ins.id,leccionId).subscribe({next:nuevo=>{this.procesando.set(false);this.seleccionada.set(nuevo);this.inscripciones.update(list=>list.map(x=>x.id===nuevo.id?nuevo:x));this.abrir(nuevo);this.mensaje.set('Lección marcada como completada.')},error:e=>{this.procesando.set(false);this.error.set(mensajeError(e))}})
  }
  completada(leccionId:number){return !!this.seleccionada()?.progresos?.some(p=>p.leccionId===leccionId&&p.completada)}
  solicitarCertificado(){const ins=this.seleccionada();if(!ins||this.procesando())return;this.procesando.set(true);this.error.set('');
    this.api.solicitarCertificado(ins.id).subscribe({next:c=>{this.procesando.set(false);this.certificado.set(c);this.mensaje.set(c.estado==='DISPONIBLE'?'Certificado disponible para descargar.':'Certificado solicitado. Pendiente de pago.')},error:e=>{this.procesando.set(false);this.error.set(mensajeError(e))}})
  }
  pagarCertificado(){const ins=this.seleccionada(),id=this.account.id();if(!ins||!id||this.procesando())return;this.procesando.set(true);this.error.set('');
    this.api.crearOrdenCertificado(ins.id,id,ins.cursoId).subscribe({next:orden=>{
      this.api.iniciarPagoCertificado(orden.id).subscribe({next:p=>{this.procesando.set(false);const url=p.sandboxInitPoint||p.initPoint;
        if(url&&/^https:\/\//.test(url)){window.open(url,'_blank','noopener,noreferrer');this.mensaje.set('Se abrió Mercado Pago sandbox. La confirmación depende del proveedor.')}
        else this.mensaje.set(`Orden de certificado #${orden.id} creada. No existe URL de checkout configurada.`)
      },error:e=>{this.procesando.set(false);this.error.set(mensajeError(e))}})
    },error:e=>{this.procesando.set(false);this.error.set(mensajeError(e))}})
  }
  descargar(){const id=this.seleccionada()?.id;if(!id)return;this.api.descargarCertificado(id).subscribe({next:blob=>{
    const url=URL.createObjectURL(blob);const anchor=document.createElement('a');anchor.href=url;anchor.download=`certificado-kido-${id}.pdf`;anchor.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
  },error:e=>this.error.set(mensajeError(e))})}
  abrirMaterial(mat:Material){
    if(!mat.url)return;
    if(/^https:\/\//i.test(mat.url)){window.open(mat.url,'_blank','noopener,noreferrer');return}
    if(!mat.url.startsWith('/api/v1/materiales/archivos/')){this.error.set('La ubicación de este material no es compatible.');return}
    this.api.descargarMaterial(mat.url).subscribe({next:blob=>{const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=mat.nombre||'material-kido';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000)},error:e=>this.error.set(mensajeError(e))})
  }
  solicitarReembolso(orden:Orden){if(orden.estado!=='APROBADA'||this.procesando())return;this.motivoReembolso='';this.error.set('');this.reembolso.set(orden)}
  leer(n:Notificacion){this.api.leerNotificacion(n.id).subscribe({next:actual=>this.notificaciones.update(list=>list.map(x=>x.id===actual.id?actual:x)),error:e=>this.error.set(mensajeError(e))})}
  cerrarSesion(){void this.auth.logout()}
}
