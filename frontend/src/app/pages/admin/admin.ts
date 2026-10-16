import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, catchError, of, Observable } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../core/services/auth';
import { Api, mensajeError } from '../../core/services/api';
import { Curso, Orden, Reembolso, Retiro } from '../../core/models/api.models';
@Component({selector:'app-admin',standalone:true,imports:[RouterLink,FormsModule],templateUrl:'./admin.html',styleUrl:'./admin.scss'})
export class Admin implements OnInit {
 readonly auth=inject(Auth);private readonly api=inject(Api);
 readonly cursos=signal<Curso[]>([]);readonly ordenes=signal<Orden[]>([]);readonly retiros=signal<Retiro[]>([]);readonly reembolsos=signal<Reembolso[]>([]);
 readonly cargando=signal(true);readonly procesando=signal(false);readonly error=signal('');readonly mensaje=signal('');
 readonly tab=signal<'cursos'|'finanzas'|'usuarios'>('cursos');
 readonly fallos=signal<string[]>([]);readonly dialogo=signal<{grupo:'curso'|'retiro'|'reembolso';id:number;accion:'aprobar'|'rechazar'|'pagar'}|null>(null);dato='';confirmado=false;
 private parcial<T>(nombre:string,op:Observable<T>,vacio:T){return op.pipe(catchError(e=>{this.fallos.update(v=>[...v,`${nombre}: ${mensajeError(e)}`]);return of(vacio)}))}
 necesitaDato(){const d=this.dialogo();return d?.accion==='rechazar'||d?.accion==='pagar'}
 tituloDialogo(){const d=this.dialogo();return d?`${d.accion==='aprobar'?'Aprobar':d.accion==='rechazar'?'Rechazar':'Registrar pago de'} ${d.grupo} #${d.id}`:''}
 abrirDialogo(grupo:'curso'|'retiro'|'reembolso',id:number,accion:'aprobar'|'rechazar'|'pagar'){if(this.procesando())return;this.dato='';this.confirmado=false;this.error.set('');this.dialogo.set({grupo,id,accion})}
 confirmarDialogo(){const d=this.dialogo();if(!d||this.procesando()||(this.necesitaDato()&&!this.dato.trim())||(d.accion==='pagar'&&!this.confirmado))return;
 const op:Observable<unknown>=d.grupo==='curso'?(d.accion==='aprobar'?this.api.aprobarCurso(d.id):this.api.rechazarCurso(d.id,this.dato.trim())):d.grupo==='retiro'?(d.accion==='aprobar'?this.api.aprobarRetiro(d.id):d.accion==='pagar'?this.api.pagarRetiro(d.id,this.dato.trim()):this.api.rechazarRetiro(d.id,this.dato.trim())):(d.accion==='aprobar'?this.api.aprobarReembolso(d.id):this.api.rechazarReembolso(d.id,this.dato.trim()));
 this.procesando.set(true);this.error.set('');op.subscribe({next:()=>{this.procesando.set(false);this.dialogo.set(null);this.mensaje.set('Operación registrada correctamente.');this.cargar()},error:e=>{this.procesando.set(false);this.error.set(mensajeError(e))}})
 }
 ngOnInit(){this.cargar()}
 cargar(){this.cargando.set(true);this.error.set('');this.fallos.set([]);forkJoin({cursos:this.parcial('Cursos',this.api.cursos(),[]),ordenes:this.parcial('Órdenes',this.api.ordenes(),[]),retiros:this.parcial('Retiros',this.api.retiros(),[]),reembolsos:this.parcial('Reembolsos',this.api.reembolsos(),[])}).subscribe({next:v=>{this.cursos.set(v.cursos);this.ordenes.set(v.ordenes);this.retiros.set(v.retiros);this.reembolsos.set(v.reembolsos);this.cargando.set(false)},error:e=>{this.cargando.set(false);this.error.set(mensajeError(e))}})}
 pendientes(){return this.cursos().filter(c=>c.estado==='PENDIENTE_REVISION')}
 ejecutarCurso(id:number,accion:'aprobar'|'rechazar'){this.abrirDialogo('curso',id,accion)}
 ejecutarRetiro(id:number,accion:'aprobar'|'pagar'|'rechazar'){this.abrirDialogo('retiro',id,accion)}
 ejecutarReembolso(id:number,accion:'aprobar'|'rechazar'){this.abrirDialogo('reembolso',id,accion)}
 cerrarSesion(){void this.auth.logout()}
}
