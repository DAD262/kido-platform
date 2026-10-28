import {Component,Input} from '@angular/core';import {SolesPipe} from '../../core/pipes/estado.pipe';
@Component({selector:'app-certificado-modelo',standalone:true,imports:[SolesPipe],templateUrl:'./certificado-modelo.html',styleUrl:'./certificado-modelo.scss'})
export class CertificadoModelo {@Input() titulo='';@Input() costo=0;}
