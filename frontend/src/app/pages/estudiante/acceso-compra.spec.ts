import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { Estudiante } from './estudiante';
import { Api } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { AccountContext } from '../../core/services/account-context';
import { Docentes } from '../../core/services/docentes';
import { Inscripcion, Orden } from '../../core/models/api.models';

describe('Acceso de nuevos alumnos después de comprar',()=>{
 let page:Estudiante;let perfil:number;let api:{inscripcionesEstudiante:jasmine.Spy;ordenesEstudiante:jasmine.Spy};
 const orden={id:35,cursoId:15,estudianteId:2024,estado:'APROBADA',inscripcionSincronizada:false} as Orden;
 const inscripcion={id:28,cursoId:15,estudianteId:2024,estado:'ACTIVA',progresos:[]} as unknown as Inscripcion;
 beforeEach(()=>{perfil=2024;api={inscripcionesEstudiante:jasmine.createSpy().and.returnValue(of([inscripcion])),ordenesEstudiante:jasmine.createSpy().and.returnValue(of([{...orden,inscripcionSincronizada:true}]))};TestBed.configureTestingModule({providers:[{provide:Api,useValue:api},{provide:Auth,useValue:{}},{provide:Docentes,useValue:{}},{provide:AccountContext,useValue:{id:()=>perfil}}]});page=TestBed.runInInjectionContext(()=>new Estudiante());});
 it('permite abrir la matrícula activa aunque la confirmación asíncrona todavía no haya llegado',()=>{page.inscripciones.set([inscripcion]);expect(page.puedeAbrirCurso(orden)).toBeTrue();});
 it('no habilita accesos revocados',()=>{page.inscripciones.set([{...inscripcion,estado:'REVOCADA'}]);expect(page.puedeAbrirCurso(orden)).toBeFalse();});
 it('actualiza los cursos después de una compra sin recargar toda la página',fakeAsync(()=>{page.ordenes.set([orden]);page.actualizarAccesosPendientes();expect(page.actualizandoAccesos()).toBeTrue();tick(3000);expect(api.inscripcionesEstudiante).toHaveBeenCalledWith(2024);expect(page.puedeAbrirCurso(orden)).toBeTrue();expect(page.actualizandoAccesos()).toBeFalse();}));
 it('funciona con el perfil autenticado de otro alumno nuevo',fakeAsync(()=>{perfil=3001;page.ordenes.set([{...orden,estudianteId:3001}]);page.actualizarAccesosPendientes();tick(3000);expect(api.inscripcionesEstudiante).toHaveBeenCalledWith(3001);expect(api.ordenesEstudiante).toHaveBeenCalledWith(3001);}));
 it('no inicia la activación automática de una compra sin aprobar',fakeAsync(()=>{page.ordenes.set([{...orden,estado:'PAGO_INICIADO'}]);page.actualizarAccesosPendientes();tick(3000);expect(api.inscripcionesEstudiante).not.toHaveBeenCalled();expect(page.actualizandoAccesos()).toBeFalse();}));
});
