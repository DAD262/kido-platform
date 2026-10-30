import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of,Subject} from 'rxjs';
import {Docente} from './docente';
import {Api} from '../../core/services/api';
import {Auth} from '../../core/services/auth';
import {AccountContext} from '../../core/services/account-context';
import {Curso,ContenidoCurso} from '../../core/models/api.models';
describe('Formulario de lección renderizado',()=>{
 it('selecciona el módulo, habilita Agregar y muestra el guardado sin perder el texto al fallar',async()=>{
  const curso={id:17,titulo:'Aplicaciones distribuidas',docenteId:1,estado:'BORRADOR'} as Curso;
  const contenido={cursoId:17,titulo:curso.titulo,modulos:[{id:46,cursoId:17,titulo:'Infraestructura Cloud',orden:1,lecciones:[]}],configuracion:{cursoId:17,progresoMinimo:100,asistenciaMinima:0,certificadoHabilitado:false,certificadoCosto:0}} as ContenidoCurso;
  const respuesta=new Subject<{id:number}>();
  const api={contenido:()=>of(contenido),crearLeccion:jasmine.createSpy().and.returnValue(respuesta),cursosDocente:()=>of([curso]),ordenesDocente:()=>of([]),saldoDocente:()=>of(null),retirosDocente:()=>of([])};
  const account={id:()=>1,estado:()=> 'listo',esManual:()=>false,asegurar:()=>Promise.resolve(null)};
  TestBed.configureTestingModule({imports:[Docente],providers:[provideRouter([]),{provide:Api,useValue:api},{provide:Auth,useValue:{nombre:'Docente'}},{provide:AccountContext,useValue:account}]});
  const fixture=TestBed.createComponent(Docente);const page=fixture.componentInstance;page.enEditor.set(true);page.seleccionado.set(curso);page.editorPaso.set(2);page.verContenido(17);fixture.detectChanges();await fixture.whenStable();
  expect(page.moduloId).toBe(46);const el=fixture.nativeElement as HTMLElement;const titulo=el.querySelector('input[placeholder="Ej.: Instalar Python"]') as HTMLInputElement;titulo.value='Docker y Kubernetes';titulo.dispatchEvent(new Event('input',{bubbles:true}));fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();
  let boton=Array.from(el.querySelectorAll('button')).find(b=>b.textContent?.includes('Agregar lección'))!;expect(boton.disabled).toBeFalse();boton.click();fixture.detectChanges();expect(api.crearLeccion).toHaveBeenCalledOnceWith(46,'Docker y Kubernetes',1,'');expect(el.textContent).toContain('Guardando lección');respuesta.error(new Error('Prueba'));fixture.detectChanges();expect(page.procesando()).toBeFalse();expect(page.leccionTitulo).toBe('Docker y Kubernetes');
 });
});
