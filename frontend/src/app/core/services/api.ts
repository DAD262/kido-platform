import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import {
  Certificado,
  ConfigCurso,
  ContenidoCurso,
  Curso,
  CursoRequest,
  DatosDocente,
  EstadoAcademico,
  Inscripcion,
  Notificacion,
  Orden,
  OrdenCertificado,
  Perfil,
  Preferencia,
  RegistroRequest,
  RegistroResponse,
  ResumenSolicitudes,
  SolicitudDocente,
  Reembolso,
  Retiro,
  Saldo,
} from '../models/api.models';

/** Errores por campo que devuelve kido-usuario-ms ({ campos: { email: '…' } }). */
export function camposError(error: unknown): Record<string, string> {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    const campos = (error.error as { campos?: unknown }).campos;
    if (campos && typeof campos === 'object') return campos as Record<string, string>;
  }
  return {};
}

/** JSON en la parte «datos» + PDF opcional en «documento», como espera @RequestPart. */
function multipart(datos: object, documento: File | null): FormData {
  const fd = new FormData();
  fd.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
  if (documento) fd.append('documento', documento, documento.name);
  return fd;
}

export function mensajeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0)
      return 'No se pudo conectar con el API Gateway. Comprueba Docker, Eureka, Gateway y el proxy de Angular.';
    if (error.status === 401)
      return 'La sesión no es válida o ha caducado. Vuelve a iniciar sesión.';
    if (error.status === 403) return 'No tienes permisos para realizar esta operación.';
    if (
      error.status === 404 &&
      !(error.error && typeof error.error === 'object' && error.error.message)
    )
      return 'El recurso no existe o no está disponible.';
    if (error.status === 503 && error.url?.includes('/usuarios/'))
      return (
        error.error?.message ??
        'El servicio de cuentas no está disponible. Revisa kido-usuario-ms y Keycloak.'
      );
    const body = error.error;
    if (typeof body === 'string') {
      const texto = body.trim();
      if (/<!doctype html>|<html/i.test(texto))
        return 'El servicio respondió con una página HTML en lugar de datos JSON. Revisa el proxy, la ruta del Gateway o si el microservicio está caído.';
      if (texto && texto.length < 240) return texto;
    }
    if (body && typeof body === 'object')
      return String(body.message ?? body.detail ?? body.error ?? `Error HTTP ${error.status}`);
    return `Error HTTP ${error.status}`;
  }
  if (error instanceof SyntaxError && /Unexpected token '\<'/i.test(error.message)) {
    return 'La aplicación recibió HTML cuando esperaba datos JSON. Suele ocurrir cuando el endpoint no existe o el Gateway devolvió una página en vez de la API.';
  }
  return error instanceof Error ? error.message : 'Ocurrió un error inesperado.';
}

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  private readonly root = '/api/v1';
  private get<T>(path: string) {
    return this.http.get<T>(this.root + path);
  }
  private post<T>(path: string, data: object = {}) {
    return this.http.post<T>(this.root + path, data);
  }
  private put<T>(path: string, data: object = {}) {
    return this.http.put<T>(this.root + path, data);
  }

  cursos() {
    return this.get<Curso[]>('/cursos');
  }
  publicados() {
    return this.get<Curso[]>('/cursos/publicados');
  }
  curso(id: number) {
    return this.get<Curso>(`/cursos/${id}`);
  }
  cursosDocente(id: number) {
    return this.get<Curso[]>(`/cursos/docente/${id}`);
  }
  contenido(id: number) {
    return this.get<ContenidoCurso>(`/cursos/${id}/contenido`);
  }
  configuracionCurso(id: number) {
    return this.get<ConfigCurso>(`/cursos/${id}/configuracion`);
  }
  crearCurso(curso: CursoRequest) {
    return this.post<Curso>('/cursos', curso);
  }
  actualizarCurso(id: number, curso: CursoRequest) {
    return this.put<Curso>(`/cursos/${id}`, curso);
  }
  revision(id: number) {
    return this.post<Curso>(`/cursos/${id}/enviar-revision`);
  }
  aprobarCurso(id: number) {
    return this.post<Curso>(`/cursos/${id}/aprobar`);
  }
  rechazarCurso(id: number, motivo: string) {
    return this.post<Curso>(`/cursos/${id}/rechazar`, { motivo });
  }
  archivarCurso(id: number) {
    return this.post<Curso>(`/cursos/${id}/archivar`);
  }
  crearModulo(id: number, titulo: string, orden: number) {
    return this.post<{ id: number }>(`/cursos/${id}/modulos`, { titulo, orden });
  }
  actualizarModulo(id: number, titulo: string, orden: number) {
    return this.put(`/modulos/${id}`, { titulo, orden });
  }
  eliminarModulo(id: number) {
    return this.http.delete(`${this.root}/modulos/${id}`);
  }
  actualizarLeccion(id: number, titulo: string, orden: number, urlContenido: string) {
    return this.put(`/lecciones/${id}`, { titulo, orden, urlContenido });
  }
  eliminarLeccion(id: number) {
    return this.http.delete(`${this.root}/lecciones/${id}`);
  }
  eliminarMaterial(id: number) {
    return this.http.delete(`${this.root}/materiales/${id}`);
  }
  subirMaterialProgreso(leccionId: number, archivo: File) {
    const data = new FormData();
    data.append('archivo', archivo);
    data.append('nombre', archivo.name);
    return this.http.post(`${this.root}/lecciones/${leccionId}/materiales/upload`, data, {
      observe: 'events',
      reportProgress: true,
    });
  }
  crearLeccion(id: number, titulo: string, orden: number, urlContenido: string) {
    return this.post<{ id: number }>(`/modulos/${id}/lecciones`, { titulo, orden, urlContenido });
  }
  configurarCurso(id: number, config: Omit<ConfigCurso, 'cursoId'>) {
    return this.put<ConfigCurso>(`/cursos/${id}/configuracion`, config);
  }

  inscripcionesEstudiante(id: number) {
    return this.get<Inscripcion[]>(`/inscripciones/estudiante/${id}`);
  }
  inscripcionesCurso(id: number) {
    return this.get<Inscripcion[]>(`/inscripciones/curso/${id}`);
  }
  crearInscripcion(
    estudianteId: number,
    cursoId: number,
    leccionIds: number[],
    config: ConfigCurso,
  ) {
    return this.post<Inscripcion>('/inscripciones', {
      estudianteId,
      cursoId,
      tipoAcceso: 'GRATUITO',
      leccionIds,
      progresoMinimo: config.progresoMinimo,
      asistenciaMinima: config.asistenciaMinima,
      certificadoHabilitado: config.certificadoHabilitado,
      certificadoCosto: config.certificadoCosto,
    });
  }
  completarLeccion(inscripcionId: number, leccionId: number) {
    return this.put<Inscripcion>(
      `/inscripciones/${inscripcionId}/lecciones/${leccionId}/completar`,
    );
  }
  estadoAcademico(id: number) {
    return this.get<EstadoAcademico>(`/inscripciones/${id}/estado-academico`);
  }
  solicitarCertificado(id: number) {
    return this.post<Certificado>(`/inscripciones/${id}/certificado`);
  }
  certificado(id: number) {
    return this.get<Certificado>(`/inscripciones/${id}/certificado`);
  }
  descargarMaterial(path: string) {
    return this.http.get(path, { responseType: 'blob' });
  }
  subirMaterial(leccionId: number, archivo: File, nombre: string) {
    const data = new FormData();
    data.append('archivo', archivo);
    if (nombre) data.append('nombre', nombre);
    return this.http.post(`${this.root}/lecciones/${leccionId}/materiales/upload`, data);
  }
  descargarCertificado(id: number) {
    return this.http.get(`${this.root}/inscripciones/${id}/certificado/pdf`, {
      responseType: 'blob',
    });
  }
  notificacionesUsuario(id: number) {
    return this.get<Notificacion[]>(`/notificaciones/usuario/${id}`);
  }
  leerNotificacion(id: number) {
    return this.put<Notificacion>(`/notificaciones/${id}/leer`);
  }

  ordenesEstudiante(id: number) {
    return this.get<Orden[]>(`/pagos/ordenes/estudiante/${id}`);
  }
  ordenesDocente(id: number) {
    return this.get<Orden[]>(`/pagos/ordenes/docente/${id}`);
  }
  ordenes() {
    return this.get<Orden[]>('/pagos/ordenes');
  }
  crearOrden(estudianteId: number, cursoId: number) {
    return this.post<Orden>('/pagos/ordenes', { estudianteId, cursoId });
  }
  iniciarPago(id: number) {
    return this.post<Preferencia>(`/pagos/ordenes/${id}/mercado-pago`);
  }
  certificadosEstudiante(id: number) {
    return this.get<OrdenCertificado[]>(`/pagos/certificados/ordenes/estudiante/${id}`);
  }
  crearOrdenCertificado(inscripcionId: number, estudianteId: number, cursoId: number) {
    return this.post<OrdenCertificado>('/pagos/certificados/ordenes', {
      inscripcionId,
      estudianteId,
      cursoId,
    });
  }
  iniciarPagoCertificado(id: number) {
    return this.post<Preferencia>(`/pagos/certificados/ordenes/${id}/mercado-pago`);
  }
  /** Confirma una orden de curso consultando el pago en Mercado Pago (el backend verifica estado, referencia y monto). */
  confirmarPago(ordenId: number, paymentId: string) {
    return this.post<Orden>(
      `/pagos/ordenes/${ordenId}/confirmar-mercado-pago?paymentId=${encodeURIComponent(paymentId)}`,
    );
  }
  confirmarPagoCertificado(ordenId: number, paymentId: string) {
    return this.post<OrdenCertificado>(
      `/pagos/certificados/ordenes/${ordenId}/confirmar-mercado-pago?paymentId=${encodeURIComponent(paymentId)}`,
    );
  }
  ordenCompra(id: number) {
    return this.get<Orden>(`/pagos/ordenes/${id}`);
  }
  saldoDocente(id: number) {
    return this.get<Saldo>(`/pagos/docentes/${id}/saldo`);
  }
  retirosDocente(id: number) {
    return this.get<Retiro[]>(`/retiros/docente/${id}`);
  }
  retiros() {
    return this.get<Retiro[]>('/retiros');
  }
  solicitarRetiro(docenteId: number, monto: number, medio: string, destino: string) {
    return this.post<Retiro>('/retiros', { docenteId, monto, medio, destino });
  }
  aprobarRetiro(id: number) {
    return this.put<Retiro>(`/retiros/${id}/aprobar`);
  }
  pagarRetiro(id: number, codigoOperacion: string) {
    return this.put<Retiro>(`/retiros/${id}/pagar`, { codigoOperacion });
  }
  rechazarRetiro(id: number, motivo: string) {
    return this.put<Retiro>(`/retiros/${id}/rechazar`, { motivo });
  }
  reembolsos() {
    return this.get<Reembolso[]>('/reembolsos');
  }
  solicitarReembolso(ordenId: number, motivo: string) {
    return this.post<Reembolso>('/reembolsos', { ordenId, motivo });
  }
  aprobarReembolso(id: number) {
    return this.put<Reembolso>(`/reembolsos/${id}/aprobar`);
  }
  rechazarReembolso(id: number, motivo: string) {
    return this.put<Reembolso>(`/reembolsos/${id}/rechazar`, { motivo });
  }

  /* ---------- Identidad: registro, perfil y verificación docente ---------- */
  registrar(datos: RegistroRequest, documento: File | null) {
    return this.http.post<RegistroResponse>(
      `${this.root}/usuarios/registro`,
      multipart(datos, documento),
    );
  }
  emailDisponible(email: string) {
    return this.http.get<{ disponible: boolean }>(`${this.root}/usuarios/registro/disponibilidad`, {
      params: new HttpParams().set('email', email),
    });
  }
  me() {
    return this.get<Perfil>('/usuarios/me');
  }
  postularDocente(datos: DatosDocente, documento: File | null) {
    return this.http.post<SolicitudDocente>(
      `${this.root}/usuarios/me/solicitud-docente`,
      multipart(datos, documento),
    );
  }
  miDocumentoDocente() {
    return this.http.get(`${this.root}/usuarios/me/solicitud-docente/documento`, {
      responseType: 'blob',
    });
  }
  solicitudesDocente(estado: string) {
    return this.http.get<SolicitudDocente[]>(`${this.root}/usuarios/admin/solicitudes`, {
      params: new HttpParams().set('estado', estado),
    });
  }
  resumenSolicitudes() {
    return this.get<ResumenSolicitudes>('/usuarios/admin/solicitudes/resumen');
  }
  documentoSolicitud(id: number) {
    return this.http.get(`${this.root}/usuarios/admin/solicitudes/${id}/documento`, {
      responseType: 'blob',
    });
  }
  revisarSolicitud(id: number, accion: 'aprobar' | 'observar' | 'rechazar', comentario: string) {
    return this.post<SolicitudDocente>(`/usuarios/admin/solicitudes/${id}/${accion}`, {
      comentario: comentario.trim() || null,
    });
  }
}
