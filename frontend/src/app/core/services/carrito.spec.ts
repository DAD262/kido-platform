import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting,HttpTestingController} from '@angular/common/http/testing';
import {Carrito} from './carrito';
import {Auth} from './auth';

describe('Carrito por cuenta',()=>{
 let carrito:Carrito;let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),{provide:Auth,useValue:{autenticado:true,subject:'estudiante-1',tieneRol:()=>true}}]});carrito=TestBed.inject(Carrito);http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 it('un rechazo queda vinculado a un solo curso',()=>{carrito.agregar(2);http.expectOne('/api/v1/pagos/carrito/2').flush({message:'Ya compraste este curso'},{status:409,statusText:'Conflict'});expect(carrito.errorCurso()).toBe(2);expect(carrito.error()).toContain('compraste');});
 it('distingue comprado de agregable con los datos del backend',()=>{carrito.cargar();http.expectOne('/api/v1/pagos/carrito').flush({items:[],total:0,moneda:'PEN',comprados:[2]});expect(carrito.comprado(2)).toBeTrue();expect(carrito.comprado(10)).toBeFalse();carrito.agregar(2);http.expectNone('/api/v1/pagos/carrito/2');carrito.agregar(10);http.expectOne('/api/v1/pagos/carrito/10').flush({items:[{cursoId:10}],total:39.9,moneda:'PEN',comprados:[2]});expect(carrito.contiene(10)).toBeTrue();});
 it('usa el resumen del backend y no suma precios enviados por el navegador',()=>{carrito.cargar();http.expectOne('/api/v1/pagos/carrito').flush({items:[{cursoId:2,titulo:'Curso',precio:50,disponible:true,motivo:''}],total:50,moneda:'PEN'});expect(carrito.cantidad()).toBe(1);expect(carrito.contiene(2)).toBeTrue();expect(carrito.datos().total).toBe(50);});
 it('agrega sin enviar estudiante ni precio y evita solicitudes simultáneas',()=>{carrito.agregar(2);carrito.agregar(2);const req=http.expectOne('/api/v1/pagos/carrito/2');expect(req.request.method).toBe('PUT');expect(req.request.body).toEqual({});req.flush({items:[],total:0,moneda:'PEN'});expect(carrito.ocupado()).toBeFalse();});
 it('conserva los cursos cuando preparar el checkout falla',()=>{carrito.datos.set({items:[{cursoId:2,titulo:'Curso',precio:50,disponible:true,motivo:''}],total:50,moneda:'PEN'});carrito.preparar(carrito.datos().items[0]).subscribe({error:()=>{}});const req=http.expectOne('/api/v1/pagos/carrito/orden');expect(req.request.body).toEqual({cursoId:2,precioAceptado:50});req.flush({message:'Precio cambió'},{status:409,statusText:'Conflict'});expect(carrito.cantidad()).toBe(1);});
 it('no muestra una respuesta de otra sesión',()=>{carrito.cargar();TestBed.inject(Auth) as Auth;Object.defineProperty(TestBed.inject(Auth),'subject',{value:'otra-cuenta',configurable:true});http.expectOne('/api/v1/pagos/carrito').flush({items:[{cursoId:2}],total:50,moneda:'PEN'});expect(carrito.cantidad()).toBe(0);});
});
