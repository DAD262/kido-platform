import {TestBed} from '@angular/core/testing';
import {Admin} from './admin';
import {Api} from '../../core/services/api';
import {Auth} from '../../core/services/auth';
import {Orden} from '../../core/models/api.models';

describe('Órdenes de administración y totales financieros',()=>{
 it('muestra pruebas e histórico pero suma únicamente producción',()=>{
  TestBed.configureTestingModule({providers:[{provide:Api,useValue:{}},{provide:Auth,useValue:{}}]});
  const admin=TestBed.runInInjectionContext(()=>new Admin());
  admin.ordenes.set([
   {id:1,estado:'APROBADA',ambiente:'PRODUCCION',montoTotal:100,comisionKido:20},
   {id:2,estado:'APROBADA',ambiente:'PRUEBAS',montoTotal:50,comisionKido:10},
   {id:3,estado:'APROBADA',ambiente:'DESCONOCIDO',montoTotal:70,comisionKido:14},
  ] as Orden[]);
  expect(admin.ordenesFiltradas().length).toBe(3);expect(admin.totalAprobado()).toBe(100);expect(admin.comisionKido()).toBe(20);
  admin.filtroAmbiente.set('PRUEBAS');expect(admin.ordenesFiltradas().map(o=>o.id)).toEqual([2]);
 });
});
