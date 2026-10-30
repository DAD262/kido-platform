import {Injectable,inject,signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
@Injectable({providedIn:'root'})
export class Docentes {
 private readonly http=inject(HttpClient);
 readonly lista=signal<{id:number;nombre:string}[]>([]);
 private iniciado=false;
 cargar(){if(this.iniciado)return;this.iniciado=true;this.http.get<{id:number;nombre:string}[]>('/api/v1/usuarios/docentes-publicos').subscribe({next:datos=>this.lista.set(datos),error:()=>{this.iniciado=false;}});}
 nombre(id:number){return this.lista().find(d=>d.id===id)?.nombre||'Docente #'+id;}
}
