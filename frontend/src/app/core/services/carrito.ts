import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { finalize, Observable } from 'rxjs';
import { Auth } from './auth';
import { mensajeError } from './api';
import { Orden } from '../models/api.models';

export interface ItemCarrito { cursoId:number; titulo:string; precio:number; disponible:boolean; motivo:string; }
export interface ResumenCarrito { items:ItemCarrito[]; total:number; moneda:string; comprados?:number[]; }
@Injectable({providedIn:'root'})
export class Carrito {
 private readonly http=inject(HttpClient);
 readonly auth=inject(Auth);
 readonly datos=signal<ResumenCarrito>({items:[],total:0,moneda:'PEN'});
 readonly ocupado=signal(false);
 readonly error=signal('');
 readonly errorCurso=signal<number|null>(null);
 readonly cantidad=computed(()=>this.datos().items.length);
 private cuenta='';
 private readonly url='/api/v1/pagos/carrito';
 constructor(){effect(()=>{const sub=this.auth.autenticado&&this.auth.tieneRol('ESTUDIANTE')?this.auth.subject:'';if(sub!==this.cuenta){this.cuenta=sub;this.datos.set({items:[],total:0,moneda:'PEN'});if(sub)this.cargar();}});}
 contiene(id:number){return this.datos().items.some(i=>i.cursoId===id);}
 comprado(id:number){return !!this.datos().comprados?.includes(id);}
 cargar(){if(!this.auth.tieneRol('ESTUDIANTE'))return;this.ejecutar(this.http.get<ResumenCarrito>(this.url));}
 agregar(id:number){if(!this.auth.autenticado){void this.auth.login('/cursos/'+id);return;}if(!this.auth.tieneRol('ESTUDIANTE')||this.comprado(id))return;this.ejecutar(this.http.put<ResumenCarrito>(this.url+'/'+id,{}),id);}
 quitar(id:number){this.ejecutar(this.http.delete<ResumenCarrito>(this.url+'/'+id));}
 vaciar(){this.ejecutar(this.http.delete<ResumenCarrito>(this.url));}
 preparar(item:ItemCarrito){return this.http.post<Orden>(this.url+'/orden',{cursoId:item.cursoId,precioAceptado:item.precio});}
 private ejecutar(op:Observable<ResumenCarrito>,cursoId:number|null=null){
  if(this.ocupado())return;
  const cuenta=this.auth.subject;this.ocupado.set(true);this.error.set('');this.errorCurso.set(null);
  op.pipe(finalize(()=>this.ocupado.set(false))).subscribe({next:datos=>{if(this.auth.subject===cuenta)this.datos.set(datos);},error:e=>{if(this.auth.subject===cuenta){this.errorCurso.set(cursoId);this.error.set(mensajeError(e));}}});
 }
}
