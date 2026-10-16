
import { Routes } from '@angular/router';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/home/home').then(m => m.Home),
    title: 'Inicio | KIDO'
  },
  {
    path: 'cursos',
    loadComponent: () =>
      import('./pages/catalogo/catalogo').then(m => m.Catalogo),
    title: 'Cursos | KIDO'
  },
  {
    path: 'cursos/:id',
    loadComponent: () => import('./pages/curso-detalle/curso-detalle').then(m => m.CursoDetalle),
    title: 'Detalle del curso | KIDO'
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login').then(m => m.Login),
    title: 'Iniciar sesión | KIDO'
  },
  {
    path: 'estudiante',
    loadComponent: () =>
      import('./pages/estudiante/estudiante').then(m => m.Estudiante),
    canActivate: [roleGuard('ESTUDIANTE')],
    title: 'Panel del estudiante | KIDO'
  },
  {
    path: 'docente',
    loadComponent: () =>
      import('./pages/docente/docente').then(m => m.Docente),
    canActivate: [roleGuard('DOCENTE')],
    title: 'Panel del docente | KIDO'
  },
  {
    path: 'admin',
    loadComponent: () =>
      import('./pages/admin/admin').then(m => m.Admin),
    canActivate: [roleGuard('ADMIN')],
    title: 'Administración | KIDO'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
