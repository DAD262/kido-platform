import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';
import { etiquetaEstado, tonoEstado } from './core/pipes/estado.pipe';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(withXhr()), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('crea la aplicación', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('muestra la marca en la barra superior', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.brand')?.textContent).toContain('kido');
  });
});

describe('Estados legibles', () => {
  it('traduce estados técnicos del backend', () => {
    expect(etiquetaEstado('PENDIENTE_REVISION')).toBe('En revisión');
    expect(etiquetaEstado('PENDING')).toBe('Pendiente');
    expect(tonoEstado('PUBLICADO')).toBe('green');
  });
  it('humaniza estados desconocidos', () => {
    expect(etiquetaEstado('NUEVO_ESTADO')).toBe('Nuevo estado');
  });
});
