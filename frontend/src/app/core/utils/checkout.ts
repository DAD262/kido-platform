import { Preferencia } from '../models/api.models';

/** Dominios de Mercado Pago a los que se permite redirigir (evita abrir URLs inesperadas). */
const DOMINIO_MP = /^https:\/\/([a-z0-9-]+\.)*mercadopago\.com(\.[a-z]{2})?\//i;
export function esCheckoutMercadoPago(url:string){return DOMINIO_MP.test(url);}

export interface PagoPendiente {
  tipo: 'curso' | 'certificado' | 'carrito';
  ordenId: number;
  cursoId: number;
  checkoutUrl?: string;
  medioId?: string;
}

const CLAVE = 'kido-pago-pendiente';

/**
 * Conserva la compra y abre el formulario de pago dentro de KIDO.
 * La aprobación se comprueba en el servidor contra Mercado Pago.
 */
export function irACheckout(p: Preferencia, pendiente: PagoPendiente): boolean {
  const url = p.checkoutUrl || p.sandboxInitPoint || p.initPoint;
  if (!url || !DOMINIO_MP.test(url)) return false;
  try {
    sessionStorage.setItem(CLAVE, JSON.stringify({...pendiente,checkoutUrl:p.retornoConfigurado===false?url:undefined}));
  } catch {
    /* sin almacenamiento: la página de resultado usa external_reference */
  }
  window.location.assign('/pago/interno/'+pendiente.tipo+'/'+pendiente.ordenId+(pendiente.medioId?'?medio='+encodeURIComponent(pendiente.medioId):''));
  return true;
}

export function leerPagoPendiente(): PagoPendiente | null {
  try {
    const v = JSON.parse(sessionStorage.getItem(CLAVE) ?? 'null');
    return v && typeof v.ordenId === 'number' ? (v as PagoPendiente) : null;
  } catch {
    return null;
  }
}

export function limpiarPagoPendiente(): void {
  try {
    sessionStorage.removeItem(CLAVE);
  } catch {
    /* nada que limpiar */
  }
}
