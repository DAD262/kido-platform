import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { Orden,CompraCarrito } from '../../core/models/api.models';
import { Api, mensajeError } from '../../core/services/api';
import { leerPagoPendiente, limpiarPagoPendiente,esCheckoutMercadoPago } from '../../core/utils/checkout';
import { Icon } from '../../shared/icon/icon';
import { SolesPipe } from '../../core/pipes/estado.pipe';

type Resultado = 'verificando' | 'listo' | 'aprobado' | 'pendiente' | 'rechazado' | 'error';

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
  imports: [RouterLink, Icon,SolesPipe],
  templateUrl: './pago-resultado.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './pago-resultado.scss',
})
export class PagoResultado implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(Api);

  readonly estado = signal<Resultado>('verificando');
  readonly detalle = signal('');
  readonly tipo = signal<'curso' | 'certificado' | 'carrito'>('curso');
  readonly compraConjunta=signal<CompraCarrito|null>(null);
  readonly cursoId = signal<number | null>(null);
  readonly pagoId = signal('');
  readonly inscripcionLista = signal(false);
  readonly orden = signal<Orden | null>(null);
  readonly checkoutUrl=signal('');
  get numeroOrden(){return this.ordenId;}
  private ordenId = 0;
  private intentos = 0;

  ngOnInit() {
    const q = this.route.snapshot.queryParamMap;
    const pagoId = q.get('payment_id') ?? q.get('collection_id') ?? '';
    const ref = q.get('external_reference') ?? '';
    const pendiente = leerPagoPendiente();

    // external_reference: "<id>" para cursos, "CERT-<id>" para certificados.
    const esCert = ref.startsWith('CERT-');
    const esCarrito=ref.startsWith('CARRITO-');
    const id = Number(esCert ? ref.slice(5) : esCarrito?ref.slice(8):ref) || pendiente?.ordenId || 0;
    this.tipo.set(ref?(esCarrito?'carrito':esCert?'certificado':'curso'):(pendiente?.tipo??'curso'));
    const coincide=pendiente?.ordenId===id&&pendiente.tipo===this.tipo();
    this.cursoId.set(coincide?pendiente!.cursoId:null);
    this.pagoId.set(pagoId);
    this.ordenId = id;
    if(coincide&&pendiente?.checkoutUrl&&esCheckoutMercadoPago(pendiente.checkoutUrl))this.checkoutUrl.set(pendiente.checkoutUrl);

    if (!id) {
      this.fallar('No pudimos identificar la orden de este pago. Revisa tus compras en «Mi aprendizaje».');
      return;
    }
    if (pagoId) {
      this.confirmar();
    } else if(this.tipo()==='carrito') {
      if(this.checkoutUrl())this.api.compraCarrito(id).subscribe({next:c=>this.actualizarCarrito(c),error:e=>this.fallar(mensajeError(e))});else this.consultarCarrito();
    } else if(this.checkoutUrl() && this.tipo()==='curso') {
      this.api.ordenCompra(this.ordenId).subscribe({next:o=>this.actualizarOrden(o),error:e=>this.fallar(mensajeError(e))});
    } else if(this.checkoutUrl()) {
      this.estado.set('listo');
    } else if (this.tipo() === 'curso') {
      this.consultarOrden();
    } else {
      this.estado.set('pendiente');
    }
  }

  confirmar() {
    if (!this.pagoId()) {
      this.fallar('Mercado Pago no devolvió el número de pago.');
      return;
    }
    this.estado.set('verificando');
    if(this.tipo()==='carrito'){this.api.confirmarCompraCarrito(this.ordenId,this.pagoId()).subscribe({next:c=>this.actualizarCarrito(c),error:e=>this.fallar(mensajeError(e))});return;}
    const op: Observable<unknown> =
      this.tipo() === 'certificado'
        ? this.api.confirmarPagoCertificado(this.ordenId, this.pagoId())
        : this.api.confirmarPago(this.ordenId, this.pagoId());
    op.subscribe({
      next: (resultado) => {
        this.checkoutUrl.set('');
        this.estado.set('aprobado');
        if (this.tipo() === 'curso') this.actualizarOrden(resultado as Orden);
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

  private actualizarOrden(orden: Orden) {
    this.orden.set(orden);
    this.cursoId.set(orden.cursoId);
    this.inscripcionLista.set(orden.inscripcionSincronizada);
    if (orden.estado === 'APROBADA') {
      this.checkoutUrl.set('');limpiarPagoPendiente();
      this.estado.set('aprobado');
      if (!orden.inscripcionSincronizada && this.intentos++ < 12) setTimeout(() => this.consultarOrden(), 3000);
    } else if (orden.estado === 'RECHAZADA') this.estado.set('rechazado');
    else this.estado.set(this.checkoutUrl()?'listo':'pendiente');
  }

  private consultarOrden() {
    if(this.tipo()==='carrito'){this.consultarCarrito();return;}
    this.api.verificarOrden(this.ordenId).subscribe({next: o => this.actualizarOrden(o), error: e => this.fallar(mensajeError(e))});
  }
  revisar(){this.estado.set('verificando');this.consultarOrden();}
  private consultarCarrito(){this.api.verificarCompraCarrito(this.ordenId).subscribe({next:c=>this.actualizarCarrito(c),error:e=>this.fallar(mensajeError(e))});}
  private actualizarCarrito(c:CompraCarrito){
    this.compraConjunta.set(c);this.inscripcionLista.set(c.inscripcionesCompletadas);
    if(c.estado==='APROBADA'){this.estado.set('aprobado');this.checkoutUrl.set('');limpiarPagoPendiente();if(!c.inscripcionesCompletadas&&this.intentos++<12)setTimeout(()=>this.consultarCarrito(),3000);}
    else if(c.estado==='RECHAZADA'||c.estado==='REEMBOLSADA'||c.estado==='CONTRACARGO')this.estado.set('rechazado');
    else this.estado.set(this.checkoutUrl()?'listo':'pendiente');
  }

  private fallar(texto: string) {
    this.detalle.set(texto);
    this.estado.set('error');
  }
}
