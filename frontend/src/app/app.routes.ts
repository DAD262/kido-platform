import { Routes } from '@angular/router';
import { roleGuard, sesionGuard } from './core/guards/role.guard';

export const routes: Routes = [
 {path:'carrito',loadComponent:()=>import('./pages/carrito/carrito').then(m=>m.CarritoPagina),canActivate:[roleGuard('ESTUDIANTE')],title:'Mi carrito · KIDO'},
 {path:'condiciones-compra',loadComponent:()=>import('./pages/condiciones-compra/condiciones-compra').then(m=>m.CondicionesCompra),title:'Información de compra · KIDO'},
  {
    path: '',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
    title: 'KIDO · Aprende a tu ritmo',
  },
  {
    path: 'cursos',
    loadComponent: () => import('./pages/catalogo/catalogo').then((m) => m.Catalogo),
    title: 'Cursos · KIDO',
  },
  {
    path: 'cursos/:id',
    loadComponent: () => import('./pages/curso-detalle/curso-detalle').then((m) => m.CursoDetalle),
    title: 'Curso · KIDO',
  },
  {
    path: 'configuracion',
    loadComponent: () => import('./pages/preferencias/preferencias').then((m) => m.Preferencias),
    title: 'Preferencias · KIDO',
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
    title: 'Ingresar · KIDO',
  },
  {
    path: 'recuperar',
    loadComponent: () => import('./pages/recuperar/recuperar').then(m => m.Recuperar),
    title: 'Recuperar acceso · KIDO',
  },
  {
    path: 'registro',
    loadComponent: () => import('./pages/registro/registro').then((m) => m.Registro),
    title: 'Crear cuenta · KIDO',
  },
  {
    path: 'postular-docente',
    loadComponent: () =>
      import('./pages/postular-docente/postular-docente').then((m) => m.PostularDocente),
    canActivate: [sesionGuard],
    title: 'Postulación docente · KIDO',
  },
  {
    path: 'estudiante',
    loadComponent: () => import('./pages/estudiante/estudiante').then((m) => m.Estudiante),
    canActivate: [roleGuard('ESTUDIANTE')],
    title: 'Mi aprendizaje · KIDO',
  },
  {
    path: 'deseados',
    loadComponent: () => import('./pages/deseados/deseados').then((m) => m.ListaDeseados),
    canActivate: [roleGuard('ESTUDIANTE')],
    title: 'Mis deseados · KIDO',
  },
  {
    path: 'docente',
    loadComponent: () => import('./pages/docente/docente').then((m) => m.Docente),
    canActivate: [roleGuard('DOCENTE')],
    title: 'Enseñar · KIDO',
  },
  {
    path: 'supervisor',
    loadComponent: () => import('./pages/supervisor/supervisor').then(m => m.Supervisor),
    canActivate: [roleGuard('SUPERVISOR')],
    title: 'Supervisión · KIDO',
  },
  {
    path: 'admin',
    loadComponent: () => import('./pages/admin/admin').then((m) => m.Admin),
    canActivate: [roleGuard('ADMIN')],
    title: 'Administración · KIDO',
  },
  {
    path: 'pago/resultado',
    loadComponent: () =>
      import('./pages/pago-resultado/pago-resultado').then((m) => m.PagoResultado),
    canActivate: [roleGuard('ESTUDIANTE')],
    title: 'Resultado del pago · KIDO',
  },
  {
    path: 'compras/:id/constancia',
    loadComponent: () => import('./pages/constancia-compra/constancia-compra').then(m => m.ConstanciaCompraPagina),
    canActivate: [sesionGuard],
    title: 'Constancia de compra · KIDO',
  },
  { path: '**', redirectTo: '' },
];
