import {Component,computed,inject,OnInit,signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {finalize} from 'rxjs';
import {Carrito} from '../../core/services/carrito';
import {Api,mensajeError} from '../../core/services/api';
import {SolesPipe} from '../../core/pipes/estado.pipe';
import {CheckoutCompra,EstadoCheckout} from '../../shared/checkout-compra/checkout-compra';
import {Icon} from '../../shared/icon/icon';
import {irACheckout} from '../../core/utils/checkout';
import {portadaCurso} from '../../core/utils/curso-portada';
@Component({selector:'app-carrito',standalone:true,imports:[RouterLink,SolesPipe,CheckoutCompra,Icon],templateUrl:'./carrito.html',styleUrl:'./carrito.scss'})
export class CarritoPagina implements OnInit {
 readonly carrito=inject(Carrito);private readonly api=inject(Api);
 readonly pagando=signal(false);readonly error=signal('');readonly confirmarVaciado=signal(false);
 readonly checkout=signal<EstadoCheckout>({disponible:false,acepta:false,version:0});readonly portada=portadaCurso;
 readonly valido=computed(()=>this.carrito.datos().items.length>0&&this.carrito.datos().items.every(i=>i.disponible));
 ngOnInit(){this.carrito.cargar();}
 pagar(){
  if(this.pagando()||this.carrito.ocupado())return;
  if(!this.valido()){this.error.set('Quita los cursos no disponibles y actualiza tu carrito antes de pagar.');return;}
  if(!this.checkout().disponible||!this.checkout().acepta){this.error.set('Revisa la disponibilidad y acepta las condiciones antes de continuar.');return;}
  this.pagando.set(true);this.error.set('');
  const datos=this.carrito.datos();
  this.api.checkoutCarrito({cursos:datos.items.map(i=>i.cursoId),totalAceptado:datos.total,aceptacion:{acepta:true,version:this.checkout().version}}).pipe(finalize(()=>this.pagando.set(false))).subscribe({
   next:compra=>{if(!compra.preferencia||!irACheckout(compra.preferencia,{tipo:'carrito',ordenId:compra.id,cursoId:0}))this.error.set('No recibimos un enlace de pago válido. Tus cursos siguen en el carrito.');},
   error:e=>{this.error.set(mensajeError(e));this.carrito.cargar();}
  });
 }
}
