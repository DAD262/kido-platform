import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { SolesPipe } from '../../core/pipes/estado.pipe';
import { mensajeError } from '../../core/services/api';
import { MercadoPagoLoader, MercadoPagoSdk, BrickController, DatosFormularioPago } from '../../core/services/mercado-pago-sdk';
import { limpiarPagoPendiente } from '../../core/utils/checkout';

interface Medio { id: string; nombre: string; tipo: string; }
interface Config { publicKey: string; configurado: boolean; total: number; ambiente: string; medios: Medio[]; email: string; version: number; }
interface Resultado { id: string; estado: string; detalle: string; codigo: string; instrucciones: string; confirmado: boolean; }
@Component({ selector:'app-pago-interno', standalone:true, imports:[FormsModule,RouterLink,SolesPipe], templateUrl:'./pago-interno.html',styleUrl:'./pago-interno.scss' })
export class PagoInterno implements OnInit, OnDestroy {
 private readonly route=inject(ActivatedRoute);private readonly http=inject(HttpClient);private readonly loader=inject(MercadoPagoLoader);
 readonly config=signal<Config|null>(null);readonly medio=signal('');readonly error=signal('');readonly cargando=signal(true);readonly procesando=signal(false);readonly formularioListo=signal(false);readonly resultado=signal<Resultado|null>(null);
 readonly montando=signal(false);
 acepta=false;email='';nombres='';apellidos='';documento='';tipoDocumento='DNI';celular='';otp='';
 private sdk?:MercadoPagoSdk;private brick?:BrickController;private destruido=false;private generacion=0;private intento=crypto.randomUUID();
 readonly tipo=this.route.snapshot.paramMap.get('tipo')??'';readonly id=Number(this.route.snapshot.paramMap.get('id'));
 private get url(){return `/api/v1/pagos/checkout-interno/${encodeURIComponent(this.tipo)}/${this.id}`;}
 async ngOnInit(){
  if(!['curso','carrito','certificado'].includes(this.tipo)||!Number.isSafeInteger(this.id)||this.id<=0){this.error.set('Compra inválida.');this.cargando.set(false);return;}
  try {const c=await firstValueFrom(this.http.get<Config>(this.url));if(this.destruido)return;this.config.set(c);this.email=c.email;
   if(!c.configurado){this.error.set('El pago dentro de KIDO está pendiente de activación. No se ha realizado ningún cobro.');return;}
   const pedido=this.route.snapshot.queryParamMap.get('medio');const inicial=c.medios.find(m=>m.id===pedido)??c.medios[0];if(inicial)await this.elegir(inicial.id);
  }catch(e){this.error.set(mensajeError(e));}finally{this.cargando.set(false);}
 }
 logo(id:string){const logos:Record<string,string>={visa:'visa.svg',debvisa:'debvisa.svg',master:'master.svg',debmaster:'debmaster.svg',amex:'amex.gif',diners:'diners.gif',yape:'yape.png',pagoefectivo_atm:'pagoefectivo_atm.gif'};return logos[id]?'/payment-logos/'+logos[id]:null;}
 esTarjeta(){return !['yape','pagoefectivo_atm',''].includes(this.medio());}
 nombre(){return this.config()?.medios.find(m=>m.id===this.medio())?.nombre??'';}
 async elegir(id:string){
  if(this.procesando()||this.montando()||this.resultado()||!this.config()?.medios.some(m=>m.id===id))return;
  this.montando.set(true);
  const turno=++this.generacion;this.medio.set(id);this.error.set('');this.formularioListo.set(false);this.otp='';
  try{if(this.brick){await this.brick.unmount();this.brick=undefined;}if(!this.config()?.configurado)return;
   if(!this.sdk){const Constructor=await this.loader.cargar();if(this.destruido||turno!==this.generacion)return;this.sdk=new Constructor(this.config()!.publicKey,{locale:'es-PE'});}
   if(this.destruido||turno!==this.generacion)return;
   if(this.esTarjeta()){const brick=await this.sdk.bricks().create('cardPayment','kido-card-payment',{
    initialization:{amount:this.config()!.total,payer:{email:this.email}},
    callbacks:{onReady:()=>{if(turno===this.generacion)this.formularioListo.set(true);},onError:()=>this.error.set('Revisa los datos del formulario seguro o vuelve a elegir el medio.'),onSubmit:(datos:DatosFormularioPago)=>this.pagarTarjeta(datos)}
   });if(this.destruido||turno!==this.generacion)await brick.unmount();else this.brick=brick;}else this.formularioListo.set(true);
  }catch(e){this.error.set(e instanceof Error?e.message:'No se pudo cargar el formulario.');}finally{this.montando.set(false);}
 }
 async pagarTarjeta(datos:DatosFormularioPago){
  if(datos.payment_method_id!==this.medio()){this.error.set('La tarjeta no corresponde al medio seleccionado. Selecciona su marca antes de continuar.');throw new Error('Medio diferente');}
  return this.enviar(datos);
 }
 async pagarAlternativo(){
  if(this.procesando()||!this.config()?.configurado)return;
  if(!this.acepta){this.error.set('Acepta las condiciones antes de pagar.');return;}
  if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)){this.error.set('Escribe un correo válido.');return;}
  this.error.set('');this.procesando.set(true);
  try{let token:string|undefined;
   if(this.medio()==='yape'){if(!/^9\d{8}$/.test(this.celular)||!/^\d{6}$/.test(this.otp))throw new Error('Revisa el celular y el código de seis dígitos de Yape.');token=(await this.sdk!.yape({otp:this.otp,phoneNumber:this.celular}).create()).id;this.otp='';}
   else if(!this.nombres.trim()||!this.apellidos.trim()||!this.documento.trim())throw new Error('Completa tus nombres, apellidos y documento para generar el código de pago.');
   await this.enviar({token,payment_method_id:this.medio(),installments:1,payer:{email:this.email,first_name:this.nombres,last_name:this.apellidos,...(this.medio()==='pagoefectivo_atm'?{identification:{type:this.tipoDocumento,number:this.documento}}:{})}},true);
  }catch(e){this.error.set(e instanceof Error?e.message:'No pudimos preparar el pago.');}finally{this.procesando.set(false);this.otp='';}
 }
 private async enviar(datos:DatosFormularioPago,ocupado=false){
  if(this.resultado()||(this.procesando()&&!ocupado))throw new Error('Ya existe una operación en curso');
  if(!this.acepta){this.error.set('Acepta las condiciones antes de pagar.');throw new Error('Falta aceptación');}
  this.error.set('');this.procesando.set(true);
  try{const resultado=await firstValueFrom(this.http.post<Resultado>(this.url,{...datos,issuer_id:datos.issuer_id===undefined?null:String(datos.issuer_id),solicitudId:this.intento,aceptacion:{acepta:true,version:this.config()!.version}}));this.resultado.set(resultado);if(resultado.confirmado)limpiarPagoPendiente();}
  catch(e){if(e instanceof HttpErrorResponse&&e.status===400)this.intento=crypto.randomUUID();this.error.set(mensajeError(e));throw new Error('No se confirmó el pago. Verifica tu compra antes de volver a intentarlo.');}finally{this.procesando.set(false);}
 }
 reintentar(){if(this.resultado()?.estado!=='rejected'&&this.resultado()?.estado!=='cancelled')return;this.resultado.set(null);this.intento=crypto.randomUUID();void this.elegir(this.medio());}
 instrucciones(){const url=this.resultado()?.instrucciones??'';return /^https:\/\/([a-z0-9-]+\.)*mercadopago\.com(\.[a-z]{2})?\//i.test(url)?url:null;}
 ngOnDestroy(){this.destruido=true;++this.generacion;this.otp='';void this.brick?.unmount();}
}
