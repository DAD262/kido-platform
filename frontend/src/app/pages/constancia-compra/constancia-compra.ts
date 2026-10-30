import {Component,OnInit,inject,signal} from '@angular/core';
import {ActivatedRoute,RouterLink} from '@angular/router';
import {Api,mensajeError} from '../../core/services/api';
import {ConstanciaCompra} from '../../core/models/api.models';
import {SolesPipe} from '../../core/pipes/estado.pipe';

@Component({selector:'app-constancia-compra',standalone:true,imports:[RouterLink,SolesPipe],templateUrl:'./constancia-compra.html',styleUrl:'./constancia-compra.scss'})
export class ConstanciaCompraPagina implements OnInit {
 private readonly api=inject(Api);private readonly route=inject(ActivatedRoute);
 readonly datos=signal<ConstanciaCompra|null>(null);readonly error=signal('');readonly cargando=signal(true);readonly descargando=signal(false);
 readonly operaciones=signal<{id:string;fecha:string;medio:string;estado:string;detalle:string;total:number}[]>([]);
 private id=0;
 ngOnInit(){this.id=Number(this.route.snapshot.paramMap.get('id'));if(!Number.isSafeInteger(this.id)||this.id<=0){this.error.set('La orden no es válida.');this.cargando.set(false);return;}this.cargar(this.route.snapshot.queryParamMap.get('operacion')??undefined);this.api.operacionesCompra(this.id).subscribe({next:r=>this.operaciones.set(r),error:()=>{}});}
 cargar(operacion?:string){this.cargando.set(true);this.error.set('');this.api.constanciaCompra(this.id,operacion).subscribe({next:d=>{this.datos.set(d);this.cargando.set(false);},error:e=>{this.error.set(mensajeError(e));this.cargando.set(false);}});}
 fecha(s:string){return s.replace('T',' ').slice(0,16);}
 medio(id:string){return ({account_money:'Dinero disponible en Mercado Pago',visa:'Visa',debvisa:'Visa Débito',master:'Mastercard',debmaster:'Mastercard Débito',yape:'Yape',amex:'American Express',pagoefectivo_atm:'PagoEfectivo'} as Record<string,string>)[id]??id;}
 pdf(imprimir=false){if(this.descargando())return;this.descargando.set(true);this.api.constanciaPdf(this.id,this.datos()?.operacion).subscribe({next:b=>{this.descargando.set(false);const url=URL.createObjectURL(b);if(imprimir){window.open(url,'_blank','noopener,noreferrer');}else{const a=document.createElement('a');a.href=url;a.download=`constancia-kido-${this.id}-${this.datos()?.operacion}.pdf`;a.click();}setTimeout(()=>URL.revokeObjectURL(url),60000);},error:e=>{this.descargando.set(false);this.error.set(mensajeError(e));}});}
}
