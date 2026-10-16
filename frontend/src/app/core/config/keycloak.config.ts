import Keycloak from 'keycloak-js';

export const keycloak = new Keycloak({
  url: 'http://localhost:19090',
  realm: 'kido',
  clientId: 'kido-frontend'
});