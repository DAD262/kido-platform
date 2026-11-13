import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, Subject, NEVER, throwError } from 'rxjs';
import { Admin } from './admin';
import { Api } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { Orden } from '../../core/models/api.models';

describe('Resultado de verificar pago por orden',()=>{
 let admin:Admin;let verificar:jasmine.Spy;
 const orden={id:34,estado:'PAGO_INICIADO',ambiente:'PRUEBAS',pagosAprobadosDetectados:0} as Orden;
 beforeEach(()=>{verificar=jasmine.createSpy().and.returnValue(of(orden));TestBed.configureTestingModule({providers:[{provide:Api,useValue:{verificarOrden:verificar}},{provide:Auth,useValue:{}}]});admin=TestBed.runInInjectionContext(()=>new Admin());admin.ordenes.set([orden]);});
 it('muestra que no hay pago aprobado aunque el estado no cambie',()=>{admin.verificarCompra(orden);expect(admin.resultadosPago()[34].texto).toContain('todavía no registra un pago aprobado');expect(admin.ordenes()[0].estado).toBe('PAGO_INICIADO');expect(admin.procesando()).toBeFalse();});
 it('identifica la orden que está consultándose y evita el doble clic',()=>{const respuesta=new Subject<Orden>();verificar.and.returnValue(respuesta);admin.verificarCompra(orden);admin.verificarCompra(orden);expect(verificar).toHaveBeenCalledOnceWith(34);expect(admin.verificandoOrden()).toBe(34);expect(admin.resultadosPago()[34].texto).toContain('Consultando');respuesta.next(orden);respuesta.complete();expect(admin.verificandoOrden()).toBeNull();});
 it('explica las operaciones repetidas en la misma fila',()=>{verificar.and.returnValue(of({...orden,estado:'APROBADA',pagosAprobadosDetectados:2}));admin.verificarCompra(orden);expect(admin.resultadosPago()[34].texto).toContain('2 operaciones aprobadas');expect(admin.ordenes()[0].estado).toBe('APROBADA');});
 it('muestra errores junto a la orden y permite reintentar',()=>{verificar.and.returnValue(throwError(()=>new Error('No disponible')));admin.verificarCompra(orden);expect(admin.resultadosPago()[34].error).toBeTrue();expect(admin.procesando()).toBeFalse();expect(admin.verificandoOrden()).toBeNull();});
 it('libera el botón cuando la consulta no responde',fakeAsync(()=>{verificar.and.returnValue(NEVER);admin.verificarCompra(orden);tick(30001);expect(admin.resultadosPago()[34].texto).toContain('tardó demasiado');expect(admin.procesando()).toBeFalse();expect(admin.verificandoOrden()).toBeNull();}));
});
