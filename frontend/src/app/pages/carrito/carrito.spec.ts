import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {of,throwError,Subject} from 'rxjs';
import {CarritoPagina} from './carrito';
import {Carrito,ResumenCarrito} from '../../core/services/carrito';
import {Api} from '../../core/services/api';
import {CompraCarrito} from '../../core/models/api.models';
describe('Checkout único del carrito',()=>{
 let page:CarritoPagina;let carrito:{ocupado:ReturnType<typeof signal<boolean>>;datos:ReturnType<typeof signal<ResumenCarrito>>;cargar:jasmine.Spy};let api:{checkoutCarrito:jasmine.Spy};
 beforeEach(()=>{carrito={ocupado:signal(false),datos:signal<ResumenCarrito>({items:[{cursoId:12,titulo:'Angular',precio:89.9,disponible:true,motivo:''},{cursoId:15,titulo:'Python',precio:69.9,disponible:true,motivo:''}],total:159.8,moneda:'PEN'}),cargar:jasmine.createSpy()};api={checkoutCarrito:jasmine.createSpy().and.returnValue(of({id:1,preferencia:{checkoutUrl:'javascript:alert(1)'}}))};TestBed.configureTestingModule({providers:[{provide:Carrito,useValue:carrito},{provide:Api,useValue:api}]});page=TestBed.runInInjectionContext(()=>new CarritoPagina());});
 it('envía ambos cursos y el total en una sola solicitud',()=>{page.checkout.set({disponible:true,acepta:true,version:1});page.pagar();expect(api.checkoutCarrito).toHaveBeenCalledOnceWith({cursos:[12,15],totalAceptado:159.8,aceptacion:{acepta:true,version:1}});});
 it('no permite continuar sin aceptación',()=>{page.checkout.set({disponible:true,acepta:false,version:1});page.pagar();expect(api.checkoutCarrito).not.toHaveBeenCalled();expect(page.error()).toContain('acepta');});
 it('no elimina silenciosamente un curso no disponible para cobrar menos',()=>{carrito.datos.update(d=>({...d,items:d.items.map(i=>i.cursoId===15?{...i,disponible:false}:i)}));page.checkout.set({disponible:true,acepta:true,version:1});page.pagar();expect(api.checkoutCarrito).not.toHaveBeenCalled();expect(page.error()).toContain('no disponibles');});
 it('conserva el carrito y actualiza precios ante un fallo',()=>{page.checkout.set({disponible:true,acepta:true,version:1});api.checkoutCarrito.and.returnValue(throwError(()=>new Error('Precio cambió')));page.pagar();expect(carrito.datos().items.length).toBe(2);expect(carrito.cargar).toHaveBeenCalled();expect(page.pagando()).toBeFalse();});
 it('rechaza enlaces de pago inesperados',()=>{page.checkout.set({disponible:true,acepta:true,version:1});page.pagar();expect(page.error()).toContain('enlace de pago válido');});
 it('un doble clic no crea dos solicitudes',()=>{const respuesta=new Subject<CompraCarrito>();api.checkoutCarrito.and.returnValue(respuesta);page.checkout.set({disponible:true,acepta:true,version:1});page.pagar();page.pagar();expect(api.checkoutCarrito).toHaveBeenCalledTimes(1);respuesta.error(new Error('Prueba'));});
 it('carrito vacío no inicia checkout',()=>{carrito.datos.set({items:[],total:0,moneda:'PEN'});page.checkout.set({disponible:true,acepta:true,version:1});page.pagar();expect(api.checkoutCarrito).not.toHaveBeenCalled();});
});