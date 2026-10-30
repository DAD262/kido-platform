import { Preferencia } from '../models/api.models';

/** Dominios de Mercado Pago a los que se permite redirigir (evita abrir URLs inesperadas). */
const DOMINIO_MP = /^https:\/\/([a-z0-9-]+\.)*mercadopago\.com(\.[a-z]{2})?\//i;
export function esCheckoutMercadoPago(url:string){return DOMINIO_MP.test(url);}

export interface PagoPendiente {
  tipo: 'curso' | 'certificado' | 'carrito';
  ordenId: number;
  cursoId: number;
  checkoutUrl?: string;
}

const CLAVE = 'kido-pago-pendiente';

/**
 * Lleva al estudiante al checkout de Mercado Pago en la MISMA pestaña, para que al terminar vuelva
 * a KIDO por la back_url (/pago/resultado) y se confirme el pago. Devuelve false si la URL no es válida.
 */
export function irACheckout(p: Preferencia, pendiente: PagoPendiente): boolean {
  const url = p.checkoutUrl || p.sandboxInitPoint || p.initPoint;
  if (!url || !DOMINIO_MP.test(url)) return false;
  let guardado=true;
  try {
    sessionStorage.setItem(CLAVE, JSON.stringify({...pendiente,checkoutUrl:p.retornoConfigurado===false?url:undefined}));
  } catch {
    guardado=false;
    /* sin almacenamiento: la página de resultado usa external_reference */
  }
  if(p.retornoConfigurado===false&&guardado){
    window.location.assign('/pago/resultado?external_reference='+encodeURIComponent((pendiente.tipo==='certificado'?'CERT-':pendiente.tipo==='carrito'?'CARRITO-':'')+pendiente.ordenId));
  }else window.location.assign(url);
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
