import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NEVER, of, Subject, throwError } from 'rxjs';
import { Docente } from './docente';
import { Api } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { AccountContext } from '../../core/services/account-context';
import { Curso, Inscripcion, Orden } from '../../core/models/api.models';
import { provideRouter } from '@angular/router';

describe('Estudiantes del docente', () => {
 let pagina: Docente;
 let api: { seguimientoDocente: jasmine.Spy; cursosDocente: jasmine.Spy; ordenesDocente: jasmine.Spy; saldoDocente: jasmine.Spy; retirosDocente: jasmine.Spy };
 const curso = { id: 1, titulo: 'Java', docenteId: 1 } as Curso;
 const otro = { id: 2, titulo: 'Spring', docenteId: 1 } as Curso;
 const alumno = { id: 12, estudianteId: 7, cursoId: 1, estudianteNombre: 'Ana Torres', cursoTitulo: 'Java', estado: 'ACTIVA' } as Inscripcion;
 beforeEach(() => {
  api = { seguimientoDocente: jasmine.createSpy().and.returnValue(of({inscripciones:[alumno],compras:[],fallos:[]})), cursosDocente: jasmine.createSpy().and.returnValue(of([curso, otro])), ordenesDocente: jasmine.createSpy().and.returnValue(of([])), saldoDocente: jasmine.createSpy().and.returnValue(of(null)), retirosDocente: jasmine.createSpy().and.returnValue(of([])) };
  TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: Api, useValue: api }, { provide: Auth, useValue: {nombre:'Docente'} }, { provide: AccountContext, useValue: { id: () => 1, estado: () => 'listo', esManual: () => false, asegurar: () => Promise.resolve(null) } }] });
  pagina = TestBed.runInInjectionContext(() => new Docente());
  pagina.cursos.set([curso, otro]); pagina.seleccionado.set(curso);
 });
 it('consulta las inscripciones al abrir la pestaña aunque el curso ya esté seleccionado', () => {
  pagina.abrirAlumnos(); expect(api.seguimientoDocente).toHaveBeenCalledOnceWith(); expect(pagina.alumnos()).toEqual([alumno]); expect(pagina.tab()).toBe('alumnos');
 });
 it('actualizar el panel vuelve a consultar las inscripciones del curso visible', () => {
  pagina.abrirAlumnos(); api.seguimientoDocente.and.returnValue(of({inscripciones:[],compras:[],fallos:[]})); pagina.cargar(); expect(api.seguimientoDocente).toHaveBeenCalledTimes(2); expect(pagina.alumnos()).toEqual([]);
 });
 it('no muestra como vacía una consulta que todavía está cargando', () => {
  const respuesta = new Subject<{inscripciones:Inscripcion[];compras:[];fallos:[]}>(); api.seguimientoDocente.and.returnValue(respuesta); pagina.abrirAlumnos(); expect(pagina.cargandoAlumnos()).toBeTrue(); respuesta.next({inscripciones:[alumno],compras:[],fallos:[]}); respuesta.complete(); expect(pagina.cargandoAlumnos()).toBeFalse(); expect(pagina.alumnos()).toEqual([alumno]);
 });
 it('una respuesta anterior no sustituye los alumnos del curso nuevo', () => {
  const anterior = new Subject<{inscripciones:Inscripcion[];compras:[];fallos:[]}>(); const actual = new Subject<{inscripciones:Inscripcion[];compras:[];fallos:[]}>(); api.seguimientoDocente.and.returnValues(anterior, actual); pagina.seleccionarAlumnos(1); pagina.seleccionarAlumnos(2); anterior.next({inscripciones:[alumno],compras:[],fallos:[]}); anterior.complete(); expect(pagina.cargandoAlumnos()).toBeTrue(); expect(pagina.alumnos()).toEqual([]); actual.next({inscripciones:[{ ...alumno, id: 15, cursoId:2 }],compras:[],fallos:[]}); actual.complete(); expect(pagina.alumnos()[0].id).toBe(15);
 });
 it('muestra el fallo de consulta y permite reintentar', () => {
  api.seguimientoDocente.and.returnValue(throwError(() => new Error('Sin conexión'))); pagina.abrirAlumnos(); expect(pagina.errorAlumnos()).not.toBe(''); expect(pagina.cargandoAlumnos()).toBeFalse(); api.seguimientoDocente.and.returnValue(of({inscripciones:[alumno],compras:[],fallos:[]})); pagina.seleccionarAlumnos(1); expect(pagina.errorAlumnos()).toBe(''); expect(pagina.alumnos()).toEqual([alumno]);
 });
 it('una petición que no responde termina con un error explícito', fakeAsync(() => {
  api.seguimientoDocente.and.returnValue(NEVER); pagina.abrirAlumnos(); tick(30001); expect(pagina.cargandoAlumnos()).toBeFalse(); expect(pagina.errorAlumnos()).toContain('tardó demasiado');
 }));
 it('muestra nombres y cursos al entrar sin seleccionar un curso', async () => {
  const fixture=TestBed.createComponent(Docente);fixture.componentInstance.tab.set('alumnos');fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();
  const texto=fixture.nativeElement.textContent as string;expect(texto).toContain('Ana Torres');expect(texto).toContain('Java');expect(texto).toContain('Todos mis cursos');expect(texto).not.toContain('Elige un curso para ver a sus estudiantes');
 });
 it('cuenta cursos distintos comprados y separa órdenes pendientes', () => {
  pagina.historial.set([{id:1,estudianteId:7,estudianteNombre:'Ana Torres',cursoId:1,cursoTitulo:'Java',estado:'APROBADA',ambiente:'PRUEBAS'},{id:2,estudianteId:7,estudianteNombre:'Ana Torres',cursoId:1,cursoTitulo:'Java',estado:'APROBADA',ambiente:'DESCONOCIDO'},{id:3,estudianteId:7,estudianteNombre:'Ana Torres',cursoId:2,cursoTitulo:'Spring',estado:'PENDIENTE',ambiente:'PRUEBAS'}] as Orden[]);
  expect(pagina.comprasPorEstudiante()[0].cursos.size).toBe(1);expect(pagina.comprasPorEstudiante()[0].nombre).toBe('Ana Torres');expect(pagina.comprasAprobadas().length).toBe(2);pagina.busquedaCompra='Spring';expect(pagina.comprasFiltradas().length).toBe(1);
 });
});
