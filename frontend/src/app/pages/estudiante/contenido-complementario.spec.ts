import {TestBed} from '@angular/core/testing';
import {Estudiante} from './estudiante';
import {Api} from '../../core/services/api';
import {Auth} from '../../core/services/auth';
import {AccountContext} from '../../core/services/account-context';
import {Docentes} from '../../core/services/docentes';
import {Inscripcion,ContenidoCurso,Orden} from '../../core/models/api.models';
describe('Ampliación del contenido sin cambiar matrículas previas',()=>{
 let page:Estudiante;let api:{completarLeccion:jasmine.Spy};
 beforeEach(()=>{api={completarLeccion:jasmine.createSpy()};TestBed.configureTestingModule({providers:[{provide:Api,useValue:api},{provide:Auth,useValue:{}},{provide:Docentes,useValue:{}},{provide:AccountContext,useValue:{}}]});page=TestBed.runInInjectionContext(()=>new Estudiante());page.seleccionada.set({id:1,estado:'ACTIVA',porcentajeProgreso:100,progresos:[{leccionId:1,completada:true}]} as Inscripcion);});
 it('mantiene las nuevas lecciones como complemento sin enviar progreso ajeno',()=>{expect(page.incluida(2)).toBeFalse();page.completar(2);expect(api.completarLeccion).not.toHaveBeenCalled();expect(page.seleccionada()?.porcentajeProgreso).toBe(100);});
 it('continuar no convierte el material nuevo en un requisito retroactivo',()=>{page.contenido.set({modulos:[{lecciones:[{id:1},{id:2}]}]} as ContenidoCurso);page.continuar();expect(page.leccionActiva()).toBeNull();expect(page.mensaje()).toContain('completaste');});
 it('Mis compras separa aprobados y conserva el importe adquirido',()=>{page.ordenes.set([{id:1,cursoId:2,estado:'APROBADA',montoTotal:79.9},{id:2,cursoId:10,estado:'PAGO_INICIADO',montoTotal:39.9},{id:3,estado:'REEMBOLSADA',montoTotal:50}] as Orden[]);expect(page.comprasAprobadas().map(o=>o.cursoId)).toEqual([2]);expect(page.comprasAprobadas()[0].montoTotal).toBe(79.9);});
});
