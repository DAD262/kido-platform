import {
  docenteVacio,
  fuerzaContrasena,
  problemaContrasena,
  validarCuenta,
  validarPerfilDocente,
  validarVerificacion,
} from './registro';

describe('Reglas de registro (mismas que kido-usuario-ms)', () => {
  it('exige la política de contraseña del realm', () => {
    expect(problemaContrasena('Docente123!', 'dani@kido.local')).toBeNull();
    expect(problemaContrasena('corta1A', '')).not.toBeNull();
    expect(problemaContrasena('sinmayuscula1', '')).not.toBeNull();
    expect(problemaContrasena('Mateo2026x', 'mateo2026@correo.pe')).not.toBeNull();
  });

  it('marca cada campo faltante de la cuenta', () => {
    const e = validarCuenta({
      nombres: '',
      apellidos: 'Q',
      email: 'x',
      password: 'a',
      aceptaTerminos: false,
      dni: '',
    });
    expect(Object.keys(e).sort()).toEqual(['aceptaTerminos', 'dni', 'email', 'nombres', 'password']);
  });

  it('valida el perfil docente', () => {
    const d = docenteVacio();
    expect(Object.keys(validarPerfilDocente(d))).toContain('dni');
    Object.assign(d, {
      dni: '72345681',
      telefono: '+51 987 654 321',
      gradoAcademico: 'MAGISTER',
      especialidad: 'Matemática',
      institucion: 'UPeU',
      aniosExperiencia: 6,
      presentacion: 'x'.repeat(80),
    });
    expect(validarPerfilDocente(d)).toEqual({});
  });

  it('pide PDF y declaración jurada', () => {
    const e = validarVerificacion(docenteVacio(), null, true);
    expect(Object.keys(e).sort()).toEqual(['declaracionAceptada', 'documento']);
  });

  it('gradúa la fuerza de la contraseña', () => {
    expect(fuerzaContrasena('').nivel).toBe(0);
    expect(fuerzaContrasena('abc').nivel).toBe(1);
    expect(fuerzaContrasena('Kido-Seguro-2026').nivel).toBe(4);
  });
});
