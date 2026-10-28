export interface CuentaAdmin {
  dni: string | null;
  id: string; username: string; email: string; nombres: string; apellidos: string;
  habilitada: boolean; roles: string[]; sinAcceso: boolean; perfilId: number | null;
}
