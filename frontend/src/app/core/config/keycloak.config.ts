import Keycloak from 'keycloak-js';

/**
 * Configuración de identidad en un solo lugar.
 * Keycloak se resuelve con el hostname de la página para que funcione
 * tanto en localhost como desde otra PC de la red del laboratorio.
 */
export const KEYCLOAK_CONFIG = {
  url: `${window.location.protocol}//${window.location.hostname}:19090`,
  realm: 'kido',
  clientId: 'kido-frontend',
} as const;

export const keycloak = new Keycloak(KEYCLOAK_CONFIG);

/** URL de la consola de administración de Keycloak para el realm de KIDO. */
export const KEYCLOAK_ADMIN_URL = `${KEYCLOAK_CONFIG.url}/admin/${KEYCLOAK_CONFIG.realm}/console/`;

/** false si Keycloak no respondió al iniciar: la app sigue funcionando en modo público. */
export const identidad = { disponible: true };
