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
interface OrdenYape { id:number; montoTotal:number; estado:string; yapeOperacion?:string|null; yapeReportadoEn?:string|null; }
@Component({ selector:'app-pago-interno', standalone:true, imports:[FormsModule,RouterLink,SolesPipe], templateUrl:'./pago-interno.html',styleUrls:['./pago-interno.scss','./yape-reporte.scss'] })
export class PagoInterno implements OnInit, OnDestroy {
 private readonly route=inject(ActivatedRoute);private readonly http=inject(HttpClient);private readonly loader=inject(MercadoPagoLoader);
 readonly config=signal<Config|null>(null);readonly medio=signal('');readonly error=signal('');readonly cargando=signal(true);readonly procesando=signal(false);readonly formularioListo=signal(false);readonly resultado=signal<Resultado|null>(null);
 readonly montando=signal(false);
 readonly ordenYape=signal<OrdenYape|null>(null);
 readonly reportandoYape=signal(false);
 acepta=false;email='';nombres='';apellidos='';documento='';tipoDocumento='DNI';celular='';otp='';operacionYape='';
 private sdk?:MercadoPagoSdk;private brick?:BrickController;private destruido=false;private generacion=0;private intento=crypto.randomUUID();
 readonly tipo=this.route.snapshot.paramMap.get('tipo')??'';readonly id=Number(this.route.snapshot.paramMap.get('id'));
 private get url(){return `/api/v1/pagos/checkout-interno/${encodeURIComponent(this.tipo)}/${this.id}`;}
 async ngOnInit(){
  if(!['curso','carrito','certificado'].includes(this.tipo)||!Number.isSafeInteger(this.id)||this.id<=0){this.error.set('Compra inválida.');this.cargando.set(false);return;}
  try {const c=await firstValueFrom(this.http.get<Config>(this.url));if(this.destruido)return;this.config.set(c);this.email=c.email;
   if(!c.configurado){this.error.set('El pago dentro de KIDO está pendiente de activación. No se ha realizado ningún cobro.');return;}
   if(c.ambiente==='YAPE_MANUAL'&&this.tipo==='curso')this.ordenYape.set(await firstValueFrom(this.http.get<OrdenYape>(`/api/v1/pagos/ordenes/${this.id}`)));
   const pedido=this.route.snapshot.queryParamMap.get('medio');const inicial=c.medios.find(m=>m.id===pedido)??c.medios[0];if(inicial)await this.elegir(inicial.id);
  }catch(e){this.error.set(mensajeError(e));}finally{this.cargando.set(false);}
 }
  logo(id:string){const logos:Record<string,string>={visa:'visa.svg',debvisa:'debvisa.svg',master:'master.svg',debmaster:'debmaster.svg',amex:'amex.svg',diners:'diners.svg',yape:'yape-official-cropped.png',pagoefectivo_atm:'pagoefectivo-official.png'};return logos[id]?'/payment-logos/'+logos[id]:null;}
 yapeManual(){return this.config()?.ambiente==='YAPE_MANUAL';}
 private async cargarSdk():Promise<MercadoPagoSdk>{
  if(!this.sdk){const Constructor=await this.loader.cargar();this.sdk=new Constructor(this.config()!.publicKey,{locale:'es-PE'});}
  return this.sdk;
 }
 esTarjeta(){return !['yape','pagoefectivo_atm',''].includes(this.medio());}
 nombre(){return this.config()?.medios.find(m=>m.id===this.medio())?.nombre??'';}
 async elegir(id:string){
  if(this.procesando()||this.montando()||this.resultado()||!this.config()?.medios.some(m=>m.id===id))return;
  this.montando.set(true);
  const turno=++this.generacion;this.medio.set(id);this.error.set('');this.formularioListo.set(false);this.otp='';
  try{if(this.brick){await this.brick.unmount();this.brick=undefined;}if(!this.config()?.configurado)return;
   if(id==='yape'){this.formularioListo.set(true);return;}
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
   if(this.medio()==='yape'&&this.yapeManual()){
    if(this.tipo!=='carrito')throw new Error('Abre la ficha del curso para preparar el pago por Yape QR.');
    const orden=await firstValueFrom(this.http.post<OrdenYape>(`/api/v1/pagos/ordenes/yape/carrito/${this.id}`,{}));
    this.ordenYape.set(orden);return;
   }
   if(this.medio()==='yape'){
    // Yape vía Mercado Pago: el celular y el código solo viajan al SDK; KIDO recibe un token de un solo uso.
    const celular=this.celular.replace(/\s+/g,'');const codigo=this.otp.trim();
    if(!/^\d{9}$/.test(celular))throw new Error('Escribe tu celular de Yape: 9 dígitos.');
    if(!/^\d{6}$/.test(codigo))throw new Error('Escribe el código de aprobación de 6 dígitos que aparece en tu app Yape.');
    const sdk=await this.cargarSdk();
    let generado:{id:string}|undefined;try{generado=await sdk.yape({otp:codigo,phoneNumber:celular}).create();}catch{throw new Error('Yape no aceptó el celular o el código. Genera un código nuevo en tu app e inténtalo otra vez.');}
    if(!generado?.id)throw new Error('Yape no generó la autorización. Genera un nuevo código en tu app e inténtalo otra vez.');
    this.otp='';
    await this.enviar({token:generado.id,payment_method_id:'yape',installments:1,payer:{email:this.email}},true);
    return;
   }
   else if(!this.nombres.trim()||!this.apellidos.trim()||!this.documento.trim())throw new Error('Completa tus nombres, apellidos y documento para generar el código de pago.');
   await this.enviar({token,payment_method_id:this.medio(),installments:1,payer:{email:this.email,first_name:this.nombres,last_name:this.apellidos,...(this.medio()==='pagoefectivo_atm'?{identification:{type:this.tipoDocumento,number:this.documento}}:{})}},true);
  }catch(e){this.error.set(e instanceof Error?e.message:'No pudimos preparar el pago.');}finally{this.procesando.set(false);this.otp='';}
 }
 async reportarPagoYape(){
  const orden=this.ordenYape();if(!orden||this.reportandoYape())return;
  const operacion=this.operacionYape.trim();
  if(!/^[A-Za-z0-9]{4,48}$/.test(operacion)){this.error.set('Escribe el número completo de operación que aparece en tu Yape.');return;}
  this.error.set('');this.reportandoYape.set(true);
  try{this.ordenYape.set(await firstValueFrom(this.http.post<OrdenYape>(`/api/v1/pagos/ordenes/yape/${orden.id}/reportar`,{operacion})));}
  catch(e){this.error.set(mensajeError(e));}finally{this.reportandoYape.set(false);}
 }
 private async enviar(datos:DatosFormularioPago,ocupado=false){
  if(this.resultado()||(this.procesando()&&!ocupado))throw new Error('Ya existe una operación en curso');
  if(!this.acepta){this.error.set('Acepta las condiciones antes de pagar.');throw new Error('Falta aceptación');}
  this.error.set('');this.procesando.set(true);
  try{const resultado=await firstValueFrom(this.http.post<Resultado>(this.url,{...datos,issuer_id:datos.issuer_id===undefined?null:String(datos.issuer_id),solicitudId:this.intento,aceptacion:{acepta:true,version:this.config()!.version}}));this.resultado.set(resultado);if(resultado.confirmado)limpiarPagoPendiente();}
  catch(e){if(e instanceof HttpErrorResponse&&e.status===400)this.intento=crypto.randomUUID();this.error.set(mensajeError(e));throw new Error('No se confirmó el pago. Verifica tu compra antes de volver a intentarlo.');}finally{this.procesando.set(false);}
 }
 reintentar(){if(this.resultado()?.estado!=='rejected'&&this.resultado()?.estado!=='cancelled')return;this.resultado.set(null);this.otp='';this.intento=crypto.randomUUID();void this.elegir(this.medio());}
 mensajeRechazo(detalle:string){
  const yape=this.medio()==='yape';
  const textos:Record<string,string>={
   cc_rejected_call_for_authorize:yape?'Yape no autorizó el pago. Revisa tu app o intenta con otro medio.':'Tu banco pide autorizar este pago. Llámalo o usa otro medio.',
   cc_amount_rate_limit_exceeded:'El monto supera el límite diario de tu Yape o tarjeta. Ajusta el límite en tu app o usa otro medio.',
   cc_rejected_max_attempts:'Superaste los intentos permitidos. Espera unos minutos o usa otro medio.',
   cc_rejected_bad_filled_security_code:yape?'El código de aprobación no es válido o ya venció. Genera uno nuevo en Yape.':'El código de seguridad no es válido.',
   cc_rejected_form_error:yape?'El celular o el código no coinciden. Revisa ambos y genera un código nuevo.':'Revisa los datos ingresados.',
   cc_rejected_insufficient_amount:'Saldo insuficiente.',
  };
  return textos[detalle]??'El proveedor no aprobó esta operación. Revisa tus datos o utiliza otro medio.';
 }
 instrucciones(){const url=this.resultado()?.instrucciones??'';return /^https:\/\/([a-z0-9-]+\.)*mercadopago\.com(\.[a-z]{2})?\//i.test(url)?url:null;}
 ngOnDestroy(){this.destruido=true;++this.generacion;this.otp='';void this.brick?.unmount();}
}
