export interface Curso {
  id: number;
  titulo: string;
  descripcion: string;
  tipo: 'GRATUITO' | 'PAGO';
  precio: number;
  docenteId: number;
  estado: string;
  categoriaId: number;
  categoria: string;
  fechaCreacion?: string;
  fechaPublicacion?: string;
  motivoRechazo?: string;
}
export interface CursoRequest {
  titulo: string;
  descripcion: string;
  tipo: 'GRATUITO' | 'PAGO';
  precio: number;
  docenteId: number;
  categoriaId: number;
  estado: string;
}
export interface Material {
  id: number;
  leccionId: number;
  nombre: string;
  tipo: string;
  url: string;
  descripcion: string;
  descargable: boolean;
}
export interface Leccion {
  id: number;
  moduloId: number;
  titulo: string;
  orden: number;
  urlContenido: string;
  materiales: Material[];
}
export interface Modulo {
  id: number;
  cursoId: number;
  titulo: string;
  orden: number;
  lecciones: Leccion[];
}
export interface ConfigCurso {
  cursoId: number;
  progresoMinimo: number;
  asistenciaMinima: number;
  certificadoHabilitado: boolean;
  certificadoCosto: number;
}
export interface ContenidoCurso {
  cursoId: number;
  titulo: string;
  modulos: Modulo[];
  configuracion: ConfigCurso;
}
export interface Progreso {
  id: number;
  leccionId: number;
  completada: boolean;
  fechaCompletada?: string;
}
export interface Inscripcion {
  id: number;
  estudianteId: number;
  cursoId: number;
  tipoAcceso: string;
  estado: string;
  fechaInscripcion: string;
  porcentajeProgreso: number;
  progresos: Progreso[];
  progresoMinimo: number;
  asistenciaMinima: number;
  certificadoHabilitado: boolean;
  certificadoCosto: number;
}
export interface EstadoAcademico {
  inscripcionId: number;
  progreso: number;
  asistencia: number;
  progresoMinimo: number;
  asistenciaMinima: number;
  cumpleProgreso: boolean;
  cumpleAsistencia: boolean;
  elegibleCertificado: boolean;
  estadoInscripcion: string;
}
export interface Certificado {
  id: number;
  inscripcionId: number;
  codigo: string;
  estado: string;
  costo: number;
  pagoConfirmado: boolean;
  fechaEmision: string;
}
export interface Orden {
  id: number;
  estudianteId: number;
  cursoId: number;
  docenteId: number;
  cursoTitulo: string;
  montoTotal: number;
  comisionKido: number;
  montoDocente: number;
  estado: string;
  fechaCreacion: string;
  inscripcionSincronizada: boolean;
}
export interface OrdenCertificado {
  id: number;
  inscripcionId: number;
  estudianteId: number;
  cursoId: number;
  montoTotal: number;
  estado: string;
}
export interface Preferencia {
  preferenceId: string;
  initPoint: string;
  sandboxInitPoint: string;
  /** URL elegida por el backend según el tipo de credencial (sandbox o init_point). */
  checkoutUrl?: string | null;
}
export interface Saldo {
  docenteId: number;
  pendiente: number;
  disponible: number;
  reservado: number;
  retirado: number;
}
export interface Retiro {
  id: number;
  docenteId: number;
  monto: number;
  medio: string;
  destino: string;
  estado: string;
  fechaSolicitud: string;
  motivoRechazo?: string;
}
export interface Reembolso {
  id: number;
  ordenId: number;
  estudianteId: number;
  cursoId: number;
  monto: number;
  motivo: string;
  estado: string;
  fechaSolicitud: string;
  motivoRechazo?: string;
}
export interface Notificacion {
  id: number;
  usuarioId: number;
  asunto: string;
  mensaje: string;
  leida: boolean;
  estado: string;
  tipo: string;
  fechaCreacion: string;
}

/* ---------- Identidad (kido-usuario-ms) ---------- */
export type TipoCuenta = 'ESTUDIANTE' | 'DOCENTE';
export type EstadoSolicitud = 'PENDIENTE' | 'OBSERVADA' | 'APROBADA' | 'RECHAZADA';
export type GradoAcademico = 'TECNICO' | 'BACHILLER' | 'TITULADO' | 'MAGISTER' | 'DOCTOR';

export interface DatosDocente {
  dni: string;
  telefono: string;
  gradoAcademico: GradoAcademico | null;
  especialidad: string;
  institucion: string;
  aniosExperiencia: number | null;
  enlaceProfesional: string;
  presentacion: string;
  declaracionAceptada: boolean;
}
export interface RegistroRequest {
  tipo: TipoCuenta;
  nombres: string;
  apellidos: string;
  email: string;
  password: string;
  aceptaTerminos: boolean;
  docente: DatosDocente | null;
}
export interface RegistroResponse {
  usuario: string;
  tipo: TipoCuenta;
  solicitudDocente: EstadoSolicitud | null;
  mensaje: string;
}
export interface SolicitudDocente {
  id: number;
  perfilId: number;
  nombreCompleto: string;
  email: string | null;
  dni: string;
  telefono: string;
  gradoAcademico: GradoAcademico;
  especialidad: string;
  institucion: string;
  aniosExperiencia: number;
  enlaceProfesional: string | null;
  presentacion: string;
  estado: EstadoSolicitud;
  comentarioRevision: string | null;
  revisadoPor: string | null;
  revisadoEn: string | null;
  enviadoEn: string;
  intentos: number;
  documento: { nombre: string; tamano: number; subidoEn: string } | null;
}
/** GET /usuarios/me: id es el número que usan cursos, inscripciones y pagos. */
export interface Perfil {
  id: number;
  username: string;
  email: string | null;
  nombres: string;
  apellidos: string;
  tipoRegistro: TipoCuenta;
  docenteVerificado: boolean;
  roles: string[];
  miembroDesde: string;
  solicitudDocente: SolicitudDocente | null;
}
export interface ResumenSolicitudes {
  pendientes: number;
  observadas: number;
  aprobadas: number;
  rechazadas: number;
}
