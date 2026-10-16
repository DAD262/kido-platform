import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Certificado, ConfigCurso, ContenidoCurso, Curso, CursoRequest, EstadoAcademico, Inscripcion, Notificacion, Orden, OrdenCertificado, Preferencia, Reembolso, Retiro, Saldo } from '../models/api.models';

export function mensajeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'No se pudo conectar con el API Gateway. Comprueba Docker y el proxy de Angular.';
    if (error.status === 401) return 'La sesión no es válida o ha caducado. Vuelve a iniciar sesión.';
    if (error.status === 403) return 'No tienes permisos para realizar esta operación.';
    if (error.status === 404) return 'El recurso no existe o no está disponible.';
    const body = error.error;
    if (typeof body === 'string' && body.trim() && body.length < 240) return body;
    if (body && typeof body === 'object') return String(body.message ?? body.detail ?? body.error ?? `Error HTTP ${error.status}`);
    return `Error HTTP ${error.status}`;
  }
  return error instanceof Error ? error.message : 'Ocurrió un error inesperado.';
}

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  private readonly root = '/api/v1';
  private get<T>(path: string) { return this.http.get<T>(this.root + path); }
  private post<T>(path: string, data: object = {}) { return this.http.post<T>(this.root + path, data); }
  private put<T>(path: string, data: object = {}) { return this.http.put<T>(this.root + path, data); }

  cursos() { return this.get<Curso[]>('/cursos'); }
  publicados() { return this.get<Curso[]>('/cursos/publicados'); }
  curso(id: number) { return this.get<Curso>(`/cursos/${id}`); }
  cursosDocente(id: number) { return this.get<Curso[]>(`/cursos/docente/${id}`); }
  contenido(id: number) { return this.get<ContenidoCurso>(`/cursos/${id}/contenido`); }
  configuracionCurso(id: number) { return this.get<ConfigCurso>(`/cursos/${id}/configuracion`); }
  crearCurso(curso: CursoRequest) { return this.post<Curso>('/cursos', curso); }
  actualizarCurso(id: number, curso: CursoRequest) { return this.put<Curso>(`/cursos/${id}`, curso); }
  revision(id: number) { return this.post<Curso>(`/cursos/${id}/enviar-revision`); }
  aprobarCurso(id: number) { return this.post<Curso>(`/cursos/${id}/aprobar`); }
  rechazarCurso(id: number, motivo: string) { return this.post<Curso>(`/cursos/${id}/rechazar`, { motivo }); }
  archivarCurso(id: number) { return this.post<Curso>(`/cursos/${id}/archivar`); }
  crearModulo(id: number, titulo: string, orden: number) { return this.post<{id:number}>(`/cursos/${id}/modulos`, {titulo, orden}); }
  actualizarModulo(id: number, titulo: string, orden: number) { return this.put(`/modulos/${id}`, {titulo, orden}); }
  eliminarModulo(id: number) { return this.http.delete(`${this.root}/modulos/${id}`); }
  actualizarLeccion(id: number, titulo: string, orden: number, urlContenido: string) { return this.put(`/lecciones/${id}`, {titulo, orden, urlContenido}); }
  eliminarLeccion(id: number) { return this.http.delete(`${this.root}/lecciones/${id}`); }
  eliminarMaterial(id: number) { return this.http.delete(`${this.root}/materiales/${id}`); }
  subirMaterialProgreso(leccionId: number, archivo: File) { const data = new FormData(); data.append('archivo', archivo); data.append('nombre', archivo.name); return this.http.post(`${this.root}/lecciones/${leccionId}/materiales/upload`, data, {observe:'events', reportProgress:true}); }
  crearLeccion(id: number, titulo: string, orden: number, urlContenido: string) { return this.post<{id:number}>(`/modulos/${id}/lecciones`, {titulo, orden, urlContenido}); }
  configurarCurso(id: number, config: Omit<ConfigCurso, 'cursoId'>) { return this.put<ConfigCurso>(`/cursos/${id}/configuracion`, config); }

  inscripcionesEstudiante(id: number) { return this.get<Inscripcion[]>(`/inscripciones/estudiante/${id}`); }
  inscripcionesCurso(id: number) { return this.get<Inscripcion[]>(`/inscripciones/curso/${id}`); }
  crearInscripcion(estudianteId: number, cursoId: number, leccionIds: number[], config: ConfigCurso) {
    return this.post<Inscripcion>('/inscripciones', { estudianteId, cursoId, tipoAcceso: 'GRATUITO', leccionIds,
      progresoMinimo: config.progresoMinimo, asistenciaMinima: config.asistenciaMinima,
      certificadoHabilitado: config.certificadoHabilitado, certificadoCosto: config.certificadoCosto });
  }
  completarLeccion(inscripcionId: number, leccionId: number) { return this.put<Inscripcion>(`/inscripciones/${inscripcionId}/lecciones/${leccionId}/completar`); }
  estadoAcademico(id: number) { return this.get<EstadoAcademico>(`/inscripciones/${id}/estado-academico`); }
  solicitarCertificado(id: number) { return this.post<Certificado>(`/inscripciones/${id}/certificado`); }
  certificado(id: number) { return this.get<Certificado>(`/inscripciones/${id}/certificado`); }
  descargarMaterial(path: string) { return this.http.get(path, {responseType: 'blob'}); }
  subirMaterial(leccionId: number, archivo: File, nombre: string) { const data = new FormData(); data.append('archivo', archivo); if(nombre) data.append('nombre',nombre); return this.http.post(`${this.root}/lecciones/${leccionId}/materiales/upload`,data); }
  descargarCertificado(id: number) { return this.http.get(`${this.root}/inscripciones/${id}/certificado/pdf`, { responseType: 'blob' }); }
  notificacionesUsuario(id: number) { return this.get<Notificacion[]>(`/notificaciones/usuario/${id}`); }
  leerNotificacion(id: number) { return this.put<Notificacion>(`/notificaciones/${id}/leer`); }

  ordenesEstudiante(id: number) { return this.get<Orden[]>(`/pagos/ordenes/estudiante/${id}`); }
  ordenesDocente(id: number) { return this.get<Orden[]>(`/pagos/ordenes/docente/${id}`); }
  ordenes() { return this.get<Orden[]>('/pagos/ordenes'); }
  crearOrden(estudianteId: number, cursoId: number) { return this.post<Orden>('/pagos/ordenes', { estudianteId, cursoId }); }
  iniciarPago(id: number) { return this.post<Preferencia>(`/pagos/ordenes/${id}/mercado-pago`); }
  certificadosEstudiante(id: number) { return this.get<OrdenCertificado[]>(`/pagos/certificados/ordenes/estudiante/${id}`); }
  crearOrdenCertificado(inscripcionId: number, estudianteId: number, cursoId: number) { return this.post<OrdenCertificado>('/pagos/certificados/ordenes', {inscripcionId, estudianteId, cursoId}); }
  iniciarPagoCertificado(id: number) { return this.post<Preferencia>(`/pagos/certificados/ordenes/${id}/mercado-pago`); }
  saldoDocente(id: number) { return this.get<Saldo>(`/pagos/docentes/${id}/saldo`); }
  retirosDocente(id: number) { return this.get<Retiro[]>(`/retiros/docente/${id}`); }
  retiros() { return this.get<Retiro[]>('/retiros'); }
  solicitarRetiro(docenteId: number, monto: number, medio: string, destino: string) { return this.post<Retiro>('/retiros', {docenteId, monto, medio, destino}); }
  aprobarRetiro(id: number) { return this.put<Retiro>(`/retiros/${id}/aprobar`); }
  pagarRetiro(id: number, codigoOperacion: string) { return this.put<Retiro>(`/retiros/${id}/pagar`, { codigoOperacion }); }
  rechazarRetiro(id: number, motivo: string) { return this.put<Retiro>(`/retiros/${id}/rechazar`, {motivo}); }
  reembolsos() { return this.get<Reembolso[]>('/reembolsos'); }
  solicitarReembolso(ordenId: number, motivo: string) { return this.post<Reembolso>('/reembolsos', {ordenId, motivo}); }
  aprobarReembolso(id: number) { return this.put<Reembolso>(`/reembolsos/${id}/aprobar`); }
  rechazarReembolso(id: number, motivo: string) { return this.put<Reembolso>(`/reembolsos/${id}/rechazar`, {motivo}); }
}
