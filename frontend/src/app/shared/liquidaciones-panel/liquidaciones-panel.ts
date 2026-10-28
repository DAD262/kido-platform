import { Component,OnInit,OnChanges,Input,inject,signal,ViewChild,ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';import { forkJoin } from 'rxjs';
import { Api,mensajeError } from '../../core/services/api';import { Auth } from '../../core/services/auth';
import { Liquidacion,DetalleLiquidacion,PoliticaLiquidacion,TransferenciaLiquidacion } from '../../core/models/api.models';
import { KIDO_PIPES } from '../../core/pipes/estado.pipe';
@Component({selector:'app-liquidaciones-panel',standalone:true,imports:[FormsModule,KIDO_PIPES],templateUrl:'./liquidaciones-panel.html',styleUrl:'./liquidaciones-panel.scss'})
export class LiquidacionesPanel implements OnInit,OnChanges {
 @Input() docenteId:number|null=null;
 @ViewChild('operacion') operacion?:ElementRef<HTMLDialogElement>;
 private readonly api=inject(Api);readonly auth=inject(Auth);private iniciado=false;
 readonly lista=signal<Liquidacion[]>([]);readonly politica=signal<PoliticaLiquidacion|null>(null);readonly detalles=signal<DetalleLiquidacion[]>([]);
 readonly cargando=signal(false);readonly procesando=signal(false);readonly error=signal('');readonly mensaje=signal('');readonly elegida=signal<Liquidacion|null>(null);readonly accion=signal<'pagar'|'anular'|'detalle'>('detalle');
 corte='';motivo='';archivo:File|null=null;
 pago:TransferenciaLiquidacion={banco:'',codigoOperacion:'',fechaOperacion:'',destino:'',titular:'',monto:0,verificada:false};
 get admin(){return this.auth.tieneRol('ADMIN')&&this.docenteId===null;}
 ngOnInit(){this.iniciado=true;this.cargar();}ngOnChanges(){if(this.iniciado)this.cargar();}
 cargar(){if(!this.admin&&!this.docenteId)return;this.cargando.set(true);this.error.set('');forkJoin({lista:this.api.liquidaciones(this.admin?undefined:this.docenteId!),politica:this.api.politicaLiquidaciones()}).subscribe({next:v=>{this.lista.set(v.lista);this.politica.set(v.politica);this.corte ||= v.politica.ultimoCorte;this.cargando.set(false);},error:e=>{this.error.set(mensajeError(e));this.cargando.set(false);}});}
 generar(){if(!this.admin||!this.corte)return;this.procesando.set(true);this.error.set('');this.mensaje.set('');this.api.generarLiquidaciones(this.corte).subscribe({next:r=>{this.procesando.set(false);this.cargar();this.mensaje.set(r.creadas?r.creadas+' liquidaciones preparadas. No se realizó ninguna transferencia.':'No hay nuevos saldos conciliados para ese corte. Las liquidaciones existentes no se duplican.');},error:e=>{this.error.set(mensajeError(e));this.procesando.set(false);}});}
 abrir(l:Liquidacion,accion:'pagar'|'anular'|'detalle'){this.elegida.set(l);this.accion.set(accion);this.error.set('');this.motivo='';this.archivo=null;this.detalles.set([]);this.pago={banco:'',codigoOperacion:'',fechaOperacion:new Intl.DateTimeFormat('en-CA',{timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date()),destino:'',titular:'',monto:0,verificada:false};this.operacion?.nativeElement.showModal();if(accion==='detalle')this.api.detalleLiquidacion(l.id).subscribe({next:d=>this.detalles.set(d),error:e=>this.error.set(mensajeError(e))});}
 cerrar(){if(this.procesando())return;this.operacion?.nativeElement.close();this.elegida.set(null);this.archivo=null;}
 elegirArchivo(event:Event){const archivo=(event.target as HTMLInputElement).files?.[0]??null;if(archivo&&archivo.size>5*1024*1024){this.error.set('La constancia no debe superar 5 MB.');this.archivo=null;return;}this.archivo=archivo;}
 registrar(){const l=this.elegida();if(!l||!this.admin||this.procesando())return;if(this.accion()==='pagar'&&(!this.archivo||!this.pago.verificada||Number(this.pago.monto)!==Number(l.monto))){this.error.set('Verifica el importe exacto y adjunta la constancia de la transferencia.');return;}this.procesando.set(true);this.error.set('');const op=this.accion()==='anular'?this.api.anularLiquidacion(l.id,this.motivo):this.api.pagarLiquidacion(l.id,this.pago,this.archivo!);op.subscribe({next:()=>{this.procesando.set(false);this.cerrar();this.cargar();this.mensaje.set(this.accion()==='anular'?'Liquidación anulada; se liberó la reserva.':'Transferencia verificada registrada. La constancia está disponible para el docente.');},error:e=>{this.error.set(mensajeError(e));this.procesando.set(false);}});}
 descargar(l:Liquidacion){this.api.constanciaLiquidacion(l.id).subscribe({next:blob=>{const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='liquidacion-'+l.id+(blob.type==='application/pdf'?'.pdf':blob.type==='image/png'?'.png':'.jpg');a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);},error:e=>this.error.set(mensajeError(e))});}
}
