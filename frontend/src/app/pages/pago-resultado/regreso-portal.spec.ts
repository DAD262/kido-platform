import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PagoResultado } from './pago-resultado';
import { Api } from '../../core/services/api';

describe('Regreso al portal al finalizar una compra',()=>{
 it('mantiene el enlace al portal en todos los resultados de compra',()=>{
  TestBed.configureTestingModule({imports:[PagoResultado],providers:[provideRouter([]),{provide:Api,useValue:{}}]});
  const fixture=TestBed.createComponent(PagoResultado);fixture.detectChanges();
  for(const estado of ['aprobado','pendiente','rechazado','error','listo'] as const){fixture.componentInstance.estado.set(estado);fixture.detectChanges();const enlace=Array.from(fixture.nativeElement.querySelectorAll('a')).find((a:unknown)=>(a as HTMLAnchorElement).textContent?.includes('Volver al portal del estudiante')) as HTMLAnchorElement;expect(enlace?.getAttribute('href')).withContext(estado).toBe('/estudiante');}
 });
});
