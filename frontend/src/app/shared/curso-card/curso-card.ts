import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Curso } from '../../core/models/api.models';
@Component({selector:'app-curso-card',standalone:true,imports:[RouterLink],templateUrl:'./curso-card.html',styleUrl:'./curso-card.scss'})
export class CursoCard {readonly curso = input.required<Curso>();}
