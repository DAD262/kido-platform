import {Component,input,output,signal} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {Subject} from 'rxjs';
import {CarritoPagina} from './carrito';
import {Carrito} from '../../core/services/carrito';
import {Api} from '../../core/services/api';
import {CompraCarrito} from '../../core/models/api.models';
import {CheckoutCompra,EstadoCheckout} from '../../shared/checkout-compra/checkout-compra';
@Component({selector:'app-checkout-compra',standalone:true,template:''})
class CheckoutStub{compact=input(false);cambio=output<EstadoCheckout>();}
describe('Resumen de compra conjunta renderizado',()=>{
 it('no tiene selector ni pagos por fila y muestra un solo botón por S/ 159.80',()=>{
  const api={checkoutCarrito:jasmine.createSpy().and.returnValue(new Subject<CompraCarrito>())};
  const carrito={datos:signal({items:[{cursoId:12,titulo:'Angular',precio:89.9,disponible:true},{cursoId:15,titulo:'Automatización con Python',precio:69.9,disponible:true}],total:159.8}),ocupado:()=>false,cantidad:()=>2,error:()=>'',cargar:()=>{}};
  TestBed.configureTestingModule({imports:[CarritoPagina],providers:[provideRouter([]),{provide:Carrito,useValue:carrito},{provide:Api,useValue:api}]}).overrideComponent(CarritoPagina,{remove:{imports:[CheckoutCompra]},add:{imports:[CheckoutStub]}});
  const fixture=TestBed.createComponent(CarritoPagina);fixture.componentInstance.checkout.set({disponible:true,acepta:true,version:1});fixture.detectChanges();
  const el=fixture.nativeElement as HTMLElement;expect(el.querySelector('select')).toBeNull();expect(el.querySelectorAll('.pay-all').length).toBe(1);expect(el.querySelectorAll('.item .btn-primary').length).toBe(0);const boton=el.querySelector('.pay-all') as HTMLButtonElement;expect(boton.textContent).toMatch(/159[.,]80/);expect(boton.disabled).toBeFalse();boton.click();expect(api.checkoutCarrito).toHaveBeenCalledOnceWith({cursos:[12,15],totalAceptado:159.8,aceptacion:{acepta:true,version:1}});
 });
});
