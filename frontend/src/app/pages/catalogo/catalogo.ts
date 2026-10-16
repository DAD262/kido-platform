import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api, mensajeError } from '../../core/services/api';
import { Curso } from '../../core/models/api.models';
import { CursoCard } from '../../shared/curso-card/curso-card';
@Component({selector:'app-catalogo',standalone:true,imports:[CursoCard,FormsModule],templateUrl:'./catalogo.html',styleUrl:'./catalogo.scss'})
export class Catalogo implements OnInit {
 private readonly api=inject(Api);
 readonly cursos=signal<Curso[]>([]);readonly cargando=signal(true);readonly error=signal('');
 readonly busqueda=signal('');readonly tipo=signal('TODOS');readonly categoria=signal('TODAS');
 readonly categorias=computed(()=>[...new Set(this.cursos().map(c=>c.categoria).filter(Boolean))].sort());
 readonly filtrados=computed(()=>this.cursos().filter(c=>(this.tipo()==='TODOS'||c.tipo===this.tipo())&&(this.categoria()==='TODAS'||c.categoria===this.categoria())&&`${c.titulo} ${c.descripcion} ${c.categoria}`.toLowerCase().includes(this.busqueda().toLowerCase())));
 ngOnInit(){this.cargar()}
 cargar(){this.cargando.set(true);this.api.publicados().subscribe({next:r=>{this.cursos.set(r);this.error.set('');this.cargando.set(false)},error:e=>{this.error.set(mensajeError(e));this.cargando.set(false)}})}
}
