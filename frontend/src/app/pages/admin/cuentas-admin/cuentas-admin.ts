import { Component, ElementRef, ViewChild, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { finalize, forkJoin } from 'rxjs';
import { CuentaAdmin } from '../../../core/models/cuenta-admin';
import { mensajeError } from '../../../core/services/api';
import { problemaContrasena } from '../../../core/utils/registro';
import { Icon } from '../../../shared/icon/icon';

@Component({selector:'app-cuentas-admin', standalone:true, imports:[FormsModule,Icon],
  templateUrl:'./cuentas-admin.html',styleUrl:'./cuentas-admin.scss'})
export class CuentasAdmin implements OnInit {
  private readonly http=inject(HttpClient);
  private readonly root='/api/v1/usuarios/admin/cuentas';
  @ViewChild('dialogo',{static:true}) dialogo!:ElementRef<HTMLDialogElement>;
  readonly cuentas=signal<CuentaAdmin[]>([]);
  readonly cargando=signal(false); readonly guardando=signal(false);
  readonly error=signal(''); readonly errorFormulario=signal(''); readonly mensaje=signal('');
  readonly consulta=signal(''); readonly categoria=signal('TODAS'); readonly pagina=signal(1);
  readonly seleccionada=signal<CuentaAdmin|null>(null); readonly modo=signal<'editar'|'password'|'supervision'>('editar');
  readonly categorias=[{id:'TODAS',nombre:'Todos'},{id:'ADMIN',nombre:'Administradores'},
    {id:'SUPERVISOR',nombre:'Supervisores'},{id:'DOCENTE',nombre:'Docentes'},{id:'ESTUDIANTE',nombre:'Estudiantes'},{id:'SIN_ROL',nombre:'Sin rol'},{id:'SIN_ACCESO',nombre:'Perfiles históricos'}];
  datos={username:'',dni:'',nombres:'',apellidos:'',email:'',habilitada:true};
  readonly cursosSupervision=signal<{id:number;titulo:string}[]>([]);
  readonly cargandoSupervision=signal(false);
  readonly supervisionLista=signal(false); private lecturaSupervision=0;supervisor=false;asignados:number[]=[];
  tieneCurso(id:number){return this.asignados.includes(id);}
  alternarCurso(id:number,on:boolean){this.asignados=on?[...new Set([...this.asignados,id])]:this.asignados.filter(i=>i!==id);}
  password=''; confirmacion=''; temporal=true; mostrar=false;
  private normalizar(s:string) { return s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase(); }
  grupo(c:CuentaAdmin) { return c.sinAcceso?'SIN_ACCESO':c.roles.includes('ADMIN')?'ADMIN':c.roles.includes('SUPERVISOR')?'SUPERVISOR':c.roles.includes('DOCENTE')?'DOCENTE':c.roles.includes('ESTUDIANTE')?'ESTUDIANTE':'SIN_ROL'; }
  etiqueta(c:CuentaAdmin) { return ({ADMIN:'Administrador',SUPERVISOR:'Supervisor',DOCENTE:'Docente',ESTUDIANTE:'Estudiante',SIN_ROL:'Sin rol',SIN_ACCESO:'Histórico sin acceso'} as Record<string,string>)[this.grupo(c)]; }
  cantidad(categoria:string) { return categoria==='TODAS'?this.cuentas().length:this.cuentas().filter(c=>this.grupo(c)===categoria).length; }
  readonly filtradas=computed(()=>{const q=this.normalizar(this.consulta().trim());return this.cuentas().filter(c=>
    (this.categoria()==='TODAS'||this.grupo(c)===this.categoria())&&this.normalizar(c.username+' '+c.email+' '+c.nombres+' '+c.apellidos).includes(q));});
  readonly paginas=computed(()=>Math.max(1,Math.ceil(this.filtradas().length/15)));
  readonly visibles=computed(()=>this.filtradas().slice((this.pagina()-1)*15,this.pagina()*15));
  ngOnInit(){this.cargar();}
  filtrar(valor:string){this.categoria.set(valor);this.pagina.set(1);}
  buscar(valor:string){this.consulta.set(valor);this.pagina.set(1);}
  cargar(){if(this.cargando())return;this.cargando.set(true);this.error.set('');
    this.http.get<CuentaAdmin[]>(this.root).pipe(finalize(()=>this.cargando.set(false))).subscribe({
      next:r=>{this.cuentas.set(r);this.pagina.set(1);},error:e=>this.error.set(mensajeError(e))});}
  abrir(c:CuentaAdmin,modo:'editar'|'password'|'supervision') {
    if(c.sinAcceso)return;
    const lectura=++this.lecturaSupervision;
    this.seleccionada.set(c);this.modo.set(modo);this.errorFormulario.set('');this.mensaje.set('');
    this.datos={username:c.username,dni:c.dni??'',nombres:c.nombres,apellidos:c.apellidos,email:c.email,habilitada:c.habilitada};
    this.password='';this.confirmacion='';this.temporal=true;this.mostrar=false;
    if(modo==='supervision') {
      this.supervisor=c.roles.includes('SUPERVISOR');this.asignados=[];this.cargandoSupervision.set(true);this.supervisionLista.set(false);
      forkJoin({ids:this.http.get<number[]>(this.root+'/'+encodeURIComponent(c.id)+'/supervision'),
        cursos:this.http.get<{id:number;titulo:string}[]>('/api/v1/cursos')}).subscribe({
        next:r=>{if(lectura!==this.lecturaSupervision||this.seleccionada()?.id!==c.id)return;
          this.asignados=r.ids;this.cursosSupervision.set(r.cursos);this.supervisionLista.set(true);this.cargandoSupervision.set(false);},
        error:e=>{if(lectura!==this.lecturaSupervision)return;this.errorFormulario.set(mensajeError(e));this.cargandoSupervision.set(false);}
      });
    }
    this.dialogo.nativeElement.showModal();
  }
  cerrar(){if(this.guardando())return;++this.lecturaSupervision;this.dialogo.nativeElement.close();this.password='';this.confirmacion='';this.seleccionada.set(null);}
  cancelar(event:Event){event.preventDefault();this.cerrar();}
  enviarRecuperacion(){const c=this.seleccionada();if(!c||this.guardando())return;this.guardando.set(true);this.errorFormulario.set('');
    this.http.post<{mensaje:string}>('/api/v1/usuarios/recuperacion/solicitar',{email:c.email}).pipe(finalize(()=>this.guardando.set(false))).subscribe({next:r=>{this.guardando.set(false);this.mensaje.set(r.mensaje);this.cerrar();},error:e=>this.errorFormulario.set(mensajeError(e))});}
  guardar(){const c=this.seleccionada();if(!c||this.guardando())return;this.errorFormulario.set('');
    if(this.modo()==='supervision') {
      if(!this.supervisionLista())return;this.guardando.set(true);
      this.http.put<void>(this.root+'/'+encodeURIComponent(c.id)+'/supervision',{habilitada:this.supervisor,cursos:this.supervisor?this.asignados:[]})
        .pipe(finalize(()=>this.guardando.set(false))).subscribe({next:()=>{this.guardando.set(false);this.mensaje.set('Supervisión actualizada. La persona debe renovar su sesión para ver el nuevo acceso.');this.cerrar();this.cargar();},error:e=>this.errorFormulario.set(mensajeError(e))});
    } else if(this.modo()==='password') {
      const problema=problemaContrasena(this.password,c.email);
      if(problema){this.errorFormulario.set(problema);return;}
      if(this.password!==this.confirmacion){this.errorFormulario.set('Las contraseñas no coinciden. Revisa ambos campos.');return;}
      if(this.password!==this.password.trim()){this.errorFormulario.set('Evita espacios al inicio o al final de la contraseña.');return;}
      this.guardando.set(true);
      this.http.post<void>(this.root+'/'+encodeURIComponent(c.id)+'/password',{password:this.password,temporal:this.temporal})
        .pipe(finalize(()=>this.guardando.set(false))).subscribe({next:()=>{this.guardando.set(false);
          this.mensaje.set(this.temporal?'Contraseña restablecida. La persona deberá cambiarla al ingresar.':'Contraseña restablecida. Ya puede ingresar con la nueva clave.');this.cerrar();},
          error:e=>this.errorFormulario.set(mensajeError(e))});
    } else {
      this.guardando.set(true);
      this.http.put<CuentaAdmin>(this.root+'/'+encodeURIComponent(c.id),this.datos).pipe(finalize(()=>this.guardando.set(false))).subscribe({
        next:r=>{this.cuentas.update(list=>list.map(u=>u.id===r.id?r:u));this.guardando.set(false);this.mensaje.set('Datos de la cuenta actualizados.');this.cerrar();this.cargar();},
        error:e=>this.errorFormulario.set(mensajeError(e))});
    }
  }
}
