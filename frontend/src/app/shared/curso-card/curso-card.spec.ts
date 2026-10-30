import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {CursoCard} from './curso-card';
import {Carrito} from '../../core/services/carrito';
import {Deseados} from '../../core/services/deseados';
import {Docentes} from '../../core/services/docentes';
describe('Tarjetas de cursos y propiedad',()=>{
 beforeEach(()=>TestBed.configureTestingModule({imports:[CursoCard],providers:[provideRouter([]),{provide:Carrito,useValue:{auth:{tieneRol:()=>true},cargar:()=>{},comprado:(id:number)=>id===2,contiene:()=>false,ocupado:()=>false,error:()=> 'Ya compraste este curso',errorCurso:()=>2}},{provide:Deseados,useValue:{permitido:false,cargar:()=>{}}},{provide:Docentes,useValue:{cargar:()=>{},nombre:()=> 'Braulio Cornejo'}}]}));
 function tarjeta(id:number,tipo='PAGO'){const f=TestBed.createComponent(CursoCard);f.componentRef.setInput('curso',{id,tipo,titulo:'Curso '+id,docenteId:1,descripcion:'Práctica',precio:39.9});f.detectChanges();return f.nativeElement as HTMLElement;}
 it('no repite el error de otro curso y muestra su docente',()=>{const el=tarjeta(10);expect(el.textContent).not.toContain('Ya compraste');expect(el.textContent).toContain('Agregar al carrito');expect(el.textContent).toContain('Braulio Cornejo');});
 it('el comprado muestra acceso a Mis compras en lugar de agregar',()=>{const el=tarjeta(2);expect(el.textContent).toContain('Comprado');expect(el.textContent).not.toContain('Agregar al carrito');});
 it('el gratuito no muestra un rechazo ajeno',()=>{const el=tarjeta(9,'GRATUITO');expect(el.textContent).toContain('Gratis');expect(el.textContent).not.toContain('Ya compraste');});
});
