import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { PagoInterno } from './pago-interno';
import { MercadoPagoLoader } from '../../core/services/mercado-pago-sdk';
describe('Pago interno de KIDO',()=>{
 let http:HttpTestingController;let pagina:PagoInterno;let loader:{cargar:jasmine.Spy};
 beforeEach(()=>{loader={cargar:jasmine.createSpy()};TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:MercadoPagoLoader,useValue:loader},{provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({tipo:'carrito',id:'7'}),queryParamMap:convertToParamMap({})}}}]});http=TestBed.inject(HttpTestingController);pagina=TestBed.runInInjectionContext(()=>new PagoInterno());});
 afterEach(()=>{http.verify();pagina.ngOnDestroy();});
 it('no carga SDK ni habilita cobros sin Public Key',async()=>{const inicio=pagina.ngOnInit();http.expectOne('/api/v1/pagos/checkout-interno/carrito/7').flush({publicKey:'',configurado:false,total:149.8,ambiente:'PRUEBAS',medios:[],email:'',version:1});await inicio;expect(loader.cargar).not.toHaveBeenCalled();expect(pagina.error()).toContain('pendiente de activación');expect(pagina.procesando()).toBeFalse();});
 it('no envía pagos sin aceptar las condiciones',async()=>{pagina.config.set({publicKey:'publica',configurado:true,total:149.8,ambiente:'PRUEBAS',medios:[],email:'',version:1});await pagina.pagarAlternativo();expect(pagina.error()).toContain('Acepta');http.expectNone('/api/v1/pagos/checkout-interno/carrito/7');});
 it('una tarjeta de otra marca no inicia un cobro',async()=>{pagina.medio.set('visa');await expectAsync(pagina.pagarTarjeta({token:'token',payment_method_id:'master',installments:1,payer:{email:'test@example.test'}})).toBeRejected();http.expectNone('/api/v1/pagos/checkout-interno/carrito/7');expect(pagina.error()).toContain('no corresponde');});
 it('Yape vía Mercado Pago envía solo el token, nunca el celular ni el código',async()=>{
  const create=jasmine.createSpy().and.resolveTo({id:'yapetoken123'});const yape=jasmine.createSpy().and.returnValue({create});
  loader.cargar.and.resolveTo(class{yape=yape;bricks(){return{create:()=>Promise.reject()};}});
  pagina.config.set({publicKey:'publica',configurado:true,total:149.8,ambiente:'PRUEBAS',medios:[{id:'yape',nombre:'Yape',tipo:'debit_card'}],email:'test@example.test',version:1});
  pagina.medio.set('yape');pagina.acepta=true;pagina.email='test@example.test';pagina.celular='111111111';pagina.otp='123456';
  const pago=pagina.pagarAlternativo();await new Promise(r=>setTimeout(r));
  const req=http.expectOne('/api/v1/pagos/checkout-interno/carrito/7');
  expect(yape).toHaveBeenCalledWith({otp:'123456',phoneNumber:'111111111'});
  expect(req.request.body.token).toBe('yapetoken123');expect(req.request.body.payment_method_id).toBe('yape');expect(req.request.body.installments).toBe(1);
  expect(JSON.stringify(req.request.body)).not.toContain('123456');expect(JSON.stringify(req.request.body)).not.toContain('111111111');
  req.flush({id:'99',estado:'approved',detalle:'accredited',codigo:'',instrucciones:'',confirmado:true});await pago;
  expect(pagina.resultado()?.confirmado).toBeTrue();expect(pagina.otp).toBe('');
 });
 it('Yape valida celular y código antes de llamar al SDK',async()=>{
  pagina.config.set({publicKey:'publica',configurado:true,total:10,ambiente:'PRUEBAS',medios:[{id:'yape',nombre:'Yape',tipo:'debit_card'}],email:'',version:1});
  pagina.medio.set('yape');pagina.acepta=true;pagina.email='test@example.test';pagina.celular='12345';pagina.otp='123456';
  await pagina.pagarAlternativo();expect(pagina.error()).toContain('celular');expect(loader.cargar).not.toHaveBeenCalled();
 });
 it('no permite abrir instrucciones externas no verificadas',()=>{pagina.resultado.set({id:'1',estado:'pending',detalle:'',codigo:'',instrucciones:'https://otro.example/pagar',confirmado:false});expect(pagina.instrucciones()).toBeNull();});
});
