import { DatosDocente, GradoAcademico } from '../models/api.models';

/**
 * Reglas del registro, idénticas a las de kido-usuario-ms (ValidadorRegistro)
 * y a la passwordPolicy del realm. El backend vuelve a validarlas: esto solo
 * evita viajes y explica el error donde ocurre.
 */
export type Errores = Record<string, string>;

export interface DatosCuenta {
  dni: string;
  nombres: string;
  apellidos: string;
  email: string;
  password: string;
  aceptaTerminos: boolean;
}

export const GRADOS: { valor: GradoAcademico; etiqueta: string }[] = [
  { valor: 'TECNICO', etiqueta: 'Técnico' },
  { valor: 'BACHILLER', etiqueta: 'Bachiller' },
  { valor: 'TITULADO', etiqueta: 'Licenciado / titulado' },
  { valor: 'MAGISTER', etiqueta: 'Magíster' },
  { valor: 'DOCTOR', etiqueta: 'Doctor' },
];

export function etiquetaGrado(valor: GradoAcademico | null | undefined): string {
  return GRADOS.find((g) => g.valor === valor)?.etiqueta ?? '—';
}

export const MAX_PDF = 5 * 1024 * 1024;
const NOMBRE = /^[\p{L}][\p{L} '\-]{0,79}$/u;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export const REGLAS_CONTRASENA: { id: string; texto: string; cumple: (p: string) => boolean }[] = [
  { id: 'largo', texto: '8 caracteres o más', cumple: (p) => p.length >= 8 },
  { id: 'mayus', texto: 'Una mayúscula', cumple: (p) => /\p{Lu}/u.test(p) },
  { id: 'minus', texto: 'Una minúscula', cumple: (p) => /\p{Ll}/u.test(p) },
  { id: 'numero', texto: 'Un número', cumple: (p) => /\d/.test(p) },
];

export function problemaContrasena(password: string, email: string): string | null {
  if (password.length > 72) return 'Máximo 72 caracteres.';
  const falla = REGLAS_CONTRASENA.find((r) => !r.cumple(password));
  if (falla) return `Te falta: ${falla.texto.toLowerCase()}.`;
  const usuario = email.split('@')[0]?.toLowerCase() ?? '';
  const p = password.toLowerCase();
  if (email && (p === email.toLowerCase() || (usuario.length >= 4 && p.includes(usuario))))
    return 'No uses tu correo dentro de la contraseña.';
  return null;
}

/** 0 = vacía … 4 = fuerte. Solo orienta; la política real es la de arriba. */
export function fuerzaContrasena(password: string): { nivel: number; etiqueta: string } {
  if (!password) return { nivel: 0, etiqueta: '' };
  let puntos = REGLAS_CONTRASENA.filter((r) => r.cumple(password)).length;
  if (password.length >= 12) puntos++;
  if (/[^\p{L}\d]/u.test(password)) puntos++;
  const nivel = puntos <= 2 ? 1 : puntos <= 4 ? 2 : puntos === 5 ? 3 : 4;
  return { nivel, etiqueta: ['', 'Débil', 'Aceptable', 'Buena', 'Fuerte'][nivel] };
}

export function limpiar(valor: string | null | undefined): string {
  return (valor ?? '').trim().replace(/\s+/g, ' ');
}

export function validarCuenta(c: DatosCuenta): Errores {
  const e: Errores = {};
  if (!/^[0-9]{8}$/.test(c.dni) || /^(\d)\1{7}$/.test(c.dni)) e['dni'] = 'Escribe un DNI válido de 8 dígitos.';
  if (!NOMBRE.test(limpiar(c.nombres))) e['nombres'] = 'Escribe tus nombres (solo letras).';
  if (!NOMBRE.test(limpiar(c.apellidos))) e['apellidos'] = 'Escribe tus apellidos (solo letras).';
  if (!EMAIL.test(limpiar(c.email)))
    e['email'] = 'Escribe un correo válido, por ejemplo nombre@correo.com.';
  const pw = problemaContrasena(c.password, limpiar(c.email).toLowerCase());
  if (pw) e['password'] = pw;
  if (!c.aceptaTerminos) e['aceptaTerminos'] = 'Acepta los términos para continuar.';
  return e;
}

export function soloDigitos(valor: string, max: number): string {
  return (valor ?? '').replace(/\D/g, '').slice(0, max);
}

export function validarPerfilDocente(d: DatosDocente): Errores {
  const e: Errores = {};
  const dni = soloDigitos(d.dni, 8);
  if (dni.length !== 8 || /^(\d)\1{7}$/.test(dni)) e['dni'] = 'El DNI tiene 8 dígitos.';
  const cel = (d.telefono ?? '').replace(/[\s\-()]/g, '').replace(/^\+51/, '');
  if (!/^9\d{8}$/.test(cel)) e['telefono'] = 'Celular de 9 dígitos que empiece con 9.';
  if (!d.gradoAcademico) e['gradoAcademico'] = 'Elige tu grado académico.';
  if (!limpiar(d.especialidad)) e['especialidad'] = 'Cuéntanos qué enseñarás.';
  if (!limpiar(d.institucion)) e['institucion'] = 'Indica dónde trabajas o estudiaste.';
  const anios = d.aniosExperiencia;
  if (
    anios === null ||
    anios === undefined ||
    !Number.isInteger(+anios) ||
    +anios < 0 ||
    +anios > 60
  )
    e['aniosExperiencia'] = 'Entre 0 y 60 años.';
  const enlace = limpiar(d.enlaceProfesional);
  if (enlace && !/^https?:\/\/[^\s/$.?#].[^\s]*$/i.test(enlace))
    e['enlaceProfesional'] = 'Usa el enlace completo: https://…';
  const largo = (d.presentacion ?? '').trim().length;
  if (largo < 80) e['presentacion'] = `Escribe al menos 80 caracteres (te faltan ${80 - largo}).`;
  else if (largo > 1200) e['presentacion'] = 'Máximo 1200 caracteres.';
  return e;
}

export function validarVerificacion(
  d: DatosDocente,
  documento: File | null,
  requiereDocumento: boolean,
): Errores {
  const e: Errores = {};
  if (requiereDocumento && !documento)
    e['documento'] = 'Adjunta tu CV, título o constancia en PDF.';
  if (!d.declaracionAceptada)
    e['declaracionAceptada'] = 'Debes declarar que la información es verdadera.';
  return e;
}

/** Revisa extensión, tamaño y la firma real del archivo (%PDF-), igual que el backend. */
export async function revisarPdf(archivo: File): Promise<string | null> {
  if (!/\.pdf$/i.test(archivo.name) && archivo.type !== 'application/pdf')
    return 'Solo se aceptan archivos PDF.';
  if (archivo.size === 0) return 'El archivo está vacío.';
  if (archivo.size > MAX_PDF) return `Pesa ${pesoLegible(archivo.size)}; el máximo es 5 MB.`;
  try {
    const firma = await archivo.slice(0, 5).text();
    if (firma !== '%PDF-') return 'El archivo no es un PDF válido.';
  } catch {
    /* Si el navegador no permite leerlo, decide el servidor. */
  }
  return null;
}

export function pesoLegible(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1).replace('.', ',')} MB`;
}

/** Normaliza antes de enviar (el backend vuelve a hacerlo). */
export function normalizarDocente(d: DatosDocente): DatosDocente {
  return {
    ...d,
    dni: soloDigitos(d.dni, 8),
    telefono: (d.telefono ?? '').replace(/[\s\-()]/g, ''),
    especialidad: limpiar(d.especialidad),
    institucion: limpiar(d.institucion),
    enlaceProfesional: limpiar(d.enlaceProfesional),
    presentacion: (d.presentacion ?? '').trim(),
    aniosExperiencia: d.aniosExperiencia === null ? null : Number(d.aniosExperiencia),
  };
}

export function docenteVacio(): DatosDocente {
  return {
    dni: '',
    telefono: '',
    gradoAcademico: null,
    especialidad: '',
    institucion: '',
    aniosExperiencia: null,
    enlaceProfesional: '',
    presentacion: '',
    declaracionAceptada: false,
  };
}
