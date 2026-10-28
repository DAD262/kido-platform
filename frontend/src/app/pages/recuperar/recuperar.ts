import {Component,DestroyRef,inject,OnInit,signal} from '@angular/core';
import {FormsModule} from '@angular/forms';import {ActivatedRoute,RouterLink} from '@angular/router';
import {HttpClient} from '@angular/common/http';import {finalize} from 'rxjs';
import {AuthShell} from '../../shared/auth-shell/auth-shell';import {Icon} from '../../shared/icon/icon';
import {mensajeError} from '../../core/services/api';import {problemaContrasena} from '../../core/utils/registro';
@Component({selector:'app-recuperar',standalone:true,imports:[FormsModule,RouterLink,AuthShell,Icon],templateUrl:'./recuperar.html',styleUrl:'./recuperar.scss'})
export class Recuperar implements OnInit {
 private readonly http=inject(HttpClient);private readonly route=inject(ActivatedRoute);private readonly destroy=inject(DestroyRef);
 private readonly root='/api/v1/usuarios/recuperacion';readonly paso=signal<'correo'|'codigo'|'listo'>('correo');
 readonly ocupado=signal(false);readonly error=signal('');readonly mensaje=signal('');readonly local=signal(false);readonly espera=signal(0);
 email='';codigo='';password='';confirmacion='';mostrar=false;estudiante=true;solicitudId='';
 private timer:ReturnType<typeof setInterval>|null=null;
 constructor(){this.destroy.onDestroy(()=>{if(this.timer)clearInterval(this.timer);});}
 ngOnInit(){this.email=this.route.snapshot.queryParamMap.get('email')??'';const id=this.route.snapshot.queryParamMap.get('solicitud');
 if(id&&/^[a-f0-9-]{36}$/i.test(id)){this.solicitudId=id;this.paso.set('codigo');}
 this.http.get<{bandejaLocal:boolean}>(this.root+'/configuracion').subscribe({next:r=>this.local.set(r.bandejaLocal),error:()=>{}});}
 solicitar(){if(this.ocupado()||this.espera())return;this.error.set('');const email=this.email.trim().toLowerCase();
 if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)){this.error.set('Escribe el correo de tu cuenta.');return;}
 this.email=email;this.ocupado.set(true);
 this.http.post<{solicitudId:string;mensaje:string}>(this.root+'/solicitar',{email}).pipe(finalize(()=>this.ocupado.set(false))).subscribe({
 next:r=>{this.solicitudId=r.solicitudId;this.mensaje.set(r.mensaje);this.codigo='';this.paso.set('codigo');this.espera.set(60);
 if(this.timer)clearInterval(this.timer);this.timer=setInterval(()=>{this.espera.update(v=>Math.max(0,v-1));if(!this.espera()&&this.timer){clearInterval(this.timer);this.timer=null;}},1000);},error:e=>this.error.set(mensajeError(e))});}
 confirmar(){if(this.ocupado())return;this.error.set('');if(!/^[0-9]{6}$/.test(this.codigo)){this.error.set('Escribe el código de 6 dígitos que recibiste.');return;}
 if(!this.estudiante){const p=problemaContrasena(this.password,this.email);if(p){this.error.set(p);return;}if(this.password!==this.confirmacion){this.error.set('Las contraseñas no coinciden.');return;}}
 this.ocupado.set(true);
 this.http.post<{temporalDni:boolean;mensaje:string}>(this.root+'/confirmar',{solicitudId:this.solicitudId,codigo:this.codigo,password:this.estudiante?null:this.password})
 .pipe(finalize(()=>this.ocupado.set(false))).subscribe({next:r=>{this.password='';this.confirmacion='';this.codigo='';this.mensaje.set(r.mensaje);this.paso.set('listo');},error:e=>this.error.set(mensajeError(e))});}
 otroCorreo(){this.paso.set('correo');this.error.set('');this.mensaje.set('');this.password='';this.confirmacion='';}
}
