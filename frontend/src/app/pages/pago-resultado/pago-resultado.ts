import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { Api, mensajeError } from '../../core/services/api';
import { leerPagoPendiente, limpiarPagoPendiente } from '../../core/utils/checkout';
import { Icon } from '../../shared/icon/icon';

type Resultado = 'verificando' | 'aprobado' | 'pendiente' | 'rechazado' | 'error';

/**
 * Página de retorno de Mercado Pago (back_urls). Mercado Pago añade a la URL:
 * payment_id / collection_id, status / collection_status, external_reference, merchant_order_id…
 *
 * No se aprueba nada con lo que diga el navegador: con el payment_id se llama a
 * /confirmar-mercado-pago y el backend consulta el pago en la API de Mercado Pago
 * (estado approved, external_reference y monto). Si el webhook llegó antes, la orden
 * ya estará aprobada y la confirmación devuelve el mismo resultado.
 */
@Component({
  selector: 'app-pago-resultado',
  standalone: true,
  imports: [RouterLink, Icon],
  templateUrl: './pago-resultado.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './pago-resultado.scss',
})
export class PagoResultado implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(Api);

  readonly estado = signal<Resultado>('verificando');
  readonly detalle = signal('');
  readonly tipo = signal<'curso' | 'certificado'>('curso');
  readonly cursoId = signal<number | null>(null);
  readonly pagoId = signal('');
  private ordenId = 0;
  private intentos = 0;

  ngOnInit() {
    const q = this.route.snapshot.queryParamMap;
    const pagoId = q.get('payment_id') ?? q.get('collection_id') ?? '';
    const status = (q.get('status') ?? q.get('collection_status') ?? '').toLowerCase();
    const ref = q.get('external_reference') ?? '';
    const pendiente = leerPagoPendiente();

    // external_reference: "<id>" para cursos, "CERT-<id>" para certificados.
    const esCert = ref.startsWith('CERT-');
    const id = Number(esCert ? ref.slice(5) : ref) || pendiente?.ordenId || 0;
    this.tipo.set(esCert || pendiente?.tipo === 'certificado' ? 'certificado' : 'curso');
    this.cursoId.set(pendiente?.cursoId ?? null);
    this.pagoId.set(pagoId);
    this.ordenId = id;

    if (!id) {
      this.fallar('No pudimos identificar la orden de este pago. Revisa tus compras en «Mi aprendizaje».');
      return;
    }
    if (status === 'approved') {
      this.confirmar();
    } else if (status === 'pending' || status === 'in_process' || status === 'authorized') {
      this.estado.set('pendiente');
      limpiarPagoPendiente();
    } else {
      // rejected, cancelled, null (el estudiante volvió sin pagar)
      this.estado.set('rechazado');
      limpiarPagoPendiente();
    }
  }

  confirmar() {
    if (!this.pagoId()) {
      this.fallar('Mercado Pago no devolvió el número de pago.');
      return;
    }
    this.estado.set('verificando');
    const op: Observable<unknown> =
      this.tipo() === 'certificado'
        ? this.api.confirmarPagoCertificado(this.ordenId, this.pagoId())
        : this.api.confirmarPago(this.ordenId, this.pagoId());
    op.subscribe({
      next: () => {
        this.estado.set('aprobado');
        limpiarPagoPendiente();
      },
      error: (e) => {
        const texto = mensajeError(e);
        // Justo después de pagar, la API puede tardar unos segundos en reportar "approved".
        if (/approved/i.test(texto) && this.intentos < 3) {
          this.intentos++;
          setTimeout(() => this.confirmar(), 2500);
          return;
        }
        this.fallar(texto);
      },
    });
  }

  private fallar(texto: string) {
    this.detalle.set(texto);
    this.estado.set('error');
  }
}
