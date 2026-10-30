import { Injectable } from '@angular/core';

export interface DatosFormularioPago {
 token?: string; payment_method_id: string; installments: number; issuer_id?: string | number;
 payer: { email: string; first_name?: string; last_name?: string; identification?: { type: string; number: string } };
}
export interface BrickController { unmount(): Promise<void>; }
export interface MercadoPagoSdk {
 bricks(): { create(tipo: string, contenedor: string, opciones: unknown): Promise<BrickController> };
 yape(datos: { otp: string; phoneNumber: string }): { create(): Promise<{ id: string }> };
}
type ConstructorMp = new (key: string, opciones: { locale: string }) => MercadoPagoSdk;

@Injectable({ providedIn: 'root' })
export class MercadoPagoLoader {
 private carga?: Promise<ConstructorMp>;
 cargar(): Promise<ConstructorMp> {
  const disponible = () => (window as unknown as { MercadoPago?: ConstructorMp }).MercadoPago;
  if (disponible()) return Promise.resolve(disponible()!);
  if (!this.carga) this.carga = new Promise<ConstructorMp>((resolve, reject) => {
   const script = document.createElement('script');
   script.src = 'https://sdk.mercadopago.com/js/v2'; script.async = true;
   const timeout = setTimeout(() => { script.remove(); this.carga=undefined; reject(new Error('Mercado Pago tardó en cargar. Vuelve a intentarlo.')); }, 20000);
   script.onload = () => { clearTimeout(timeout); const sdk=disponible(); if(sdk)resolve(sdk);else{this.carga=undefined;reject(new Error('No se pudo iniciar el formulario de pago.'));} };
   script.onerror = () => { clearTimeout(timeout); script.remove(); this.carga=undefined; reject(new Error('No pudimos cargar el formulario seguro de Mercado Pago.')); };
   document.head.appendChild(script);
  });
  return this.carga;
 }
}
