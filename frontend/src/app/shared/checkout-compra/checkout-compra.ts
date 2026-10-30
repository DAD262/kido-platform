import { Component, inject, input, signal, output, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Api } from '../../core/services/api';
import { EstadoComercial, MetodoPago } from '../../core/models/api.models';
import { Icon } from '../icon/icon';
export interface EstadoCheckout { disponible: boolean; acepta: boolean; version: number; medioId?: string; }
@Component({selector:'app-checkout-compra',standalone:true,imports:[FormsModule,RouterLink,Icon],templateUrl:'./checkout-compra.html',styleUrl:'./checkout-compra.scss'})
export class CheckoutCompra implements OnInit {
 readonly compact=input(false);
 private readonly api=inject(Api);
 readonly cambio=output<EstadoCheckout>();
 readonly metodo=signal<MetodoPago|null>(null);
 readonly comercial=signal<EstadoComercial|null>(null);
 readonly cargando=signal(true);readonly error=signal('');
 readonly medioElegido=signal('');
 elegirMedio(id:string){if(this.metodo()?.medios.some(m=>m.id===id)){this.medioElegido.set(id);this.emitir();}}
 acepta=false;
 logoMedio(id:string):string|null {const logos:Record<string,string>={visa:'visa.svg',debvisa:'debvisa.svg',master:'master.svg',debmaster:'debmaster.svg',amex:'amex.gif',diners:'diners.gif',yape:'yape.png',pagoefectivo_atm:'pagoefectivo_atm.gif'};return logos[id]?'/payment-logos/'+logos[id]:null;}
 ngOnInit(){this.cargar();}
 cargar(){this.cargando.set(true);this.error.set('');this.acepta=false;this.emitir();forkJoin({metodos:this.api.metodosPago(),comercial:this.api.informacionComercial()}).subscribe({next:({metodos,comercial})=>{this.metodo.set(metodos.find(m=>m.id==='mercado-pago')??null);this.comercial.set(comercial);this.cargando.set(false);this.emitir();},error:()=>{this.metodo.set(null);this.cargando.set(false);this.error.set('No pudimos consultar los medios de pago. Inténtalo de nuevo.');this.emitir();}});}
 emitir(){this.cambio.emit({disponible:!this.cargando()&&!!this.metodo()?.disponible&&(!!this.comercial()?.lista||!!this.metodo()?.modoPruebaLocal),acepta:this.acepta,version:this.comercial()?.datos.version??0,...(this.medioElegido()?{medioId:this.medioElegido()}:{})});}
}
