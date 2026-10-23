import { Pipe, PipeTransform } from '@angular/core';

type Tono = 'green' | 'amber' | 'red' | 'blue' | 'gray';

/**
 * Traduce los estados técnicos del backend (mezcla de español e inglés,
 * p. ej. PENDIENTE_REVISION, PENDING, APPROVED) a etiquetas legibles y
 * a un tono de color coherente en toda la interfaz.
 */
const ESTADOS: Record<string, [string, Tono]> = {
  // Cursos
  BORRADOR: ['Borrador', 'gray'],
  PENDIENTE_REVISION: ['En revisión', 'amber'],
  PUBLICADO: ['Publicado', 'green'],
  RECHAZADO: ['Requiere cambios', 'red'],
  ARCHIVADO: ['Archivado', 'gray'],
  // Inscripciones
  ACTIVA: ['Activa', 'blue'],
  COMPLETADA: ['Completada', 'green'],
  CANCELADA: ['Cancelada', 'gray'],
  REVOCADA: ['Revocada', 'red'],
  PENDIENTE_PAGO: ['Pendiente de pago', 'amber'],
  PAGO_INICIADO: ['Pago en curso', 'blue'],
  ANULADO: ['Anulado', 'red'],
  COMPRA: ['Compra', 'blue'],
  // Órdenes / pagos
  PENDIENTE: ['Pendiente', 'amber'],
  // Solicitudes docentes
  OBSERVADA: ['Con observaciones', 'blue'],
  APROBADA: ['Aprobada', 'green'],
  RECHAZADA: ['Rechazada', 'red'],
  REEMBOLSADA: ['Reembolsada', 'gray'],
  EXPIRADA: ['Expirada', 'gray'],
  // Retiros
  PENDING: ['Pendiente', 'amber'],
  APPROVED: ['Aprobado', 'blue'],
  PAID: ['Pagado', 'green'],
  REJECTED: ['Rechazado', 'red'],
  // Reembolsos
  SOLICITADO: ['Solicitado', 'amber'],
  APROBADO: ['Aprobado', 'green'],
  // Certificados
  DISPONIBLE: ['Disponible', 'green'],
  EMITIDO: ['Emitido', 'green'],
  // Tipos
  GRATUITO: ['Gratis', 'green'],
  PAGO: ['De pago', 'blue'],
};

function humanizar(valor: string): string {
  const texto = valor.replace(/_/g, ' ').toLowerCase();
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

export function etiquetaEstado(valor: string | null | undefined): string {
  if (!valor) return '—';
  return ESTADOS[valor]?.[0] ?? humanizar(valor);
}

export function tonoEstado(valor: string | null | undefined): Tono {
  return (valor && ESTADOS[valor]?.[1]) || 'gray';
}

@Pipe({ name: 'estado', standalone: true })
export class EstadoPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    return etiquetaEstado(valor);
  }
}

@Pipe({ name: 'tono', standalone: true })
export class TonoPipe implements PipeTransform {
  transform(valor: string | null | undefined): Tono {
    return tonoEstado(valor);
  }
}

/** Formatea montos en soles. Acepta null/undefined/strings del backend. */
@Pipe({ name: 'soles', standalone: true })
export class SolesPipe implements PipeTransform {
  private static readonly fmt = new Intl.NumberFormat('es-PE', {
    style: 'currency',
    currency: 'PEN',
  });
  transform(valor: number | string | null | undefined): string {
    const n = Number(valor);
    return SolesPipe.fmt.format(Number.isFinite(n) ? n : 0);
  }
}

/** Fecha corta legible (ej. «12 oct 2026, 14:05»). */
@Pipe({ name: 'fecha', standalone: true })
export class FechaPipe implements PipeTransform {
  private static readonly fmt = new Intl.DateTimeFormat('es-PE', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
  transform(valor: string | null | undefined): string {
    if (!valor) return '';
    const d = new Date(valor);
    return Number.isNaN(d.getTime()) ? valor : FechaPipe.fmt.format(d);
  }
}

export const KIDO_PIPES = [EstadoPipe, TonoPipe, SolesPipe, FechaPipe] as const;
