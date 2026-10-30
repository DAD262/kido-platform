-- Mantenimiento LOCAL con Keycloak detenido. Solo el cliente de Kido.
BEGIN;
DO $$
DECLARE destino varchar;
BEGIN
 SELECT c.id INTO STRICT destino FROM client c JOIN realm r ON r.id=c.realm_id
 WHERE r.name='kido' AND c.client_id='kido-frontend';
 UPDATE client SET root_url='http://localhost:4300',base_url='http://localhost:4300/' WHERE id=destino;
 DELETE FROM redirect_uris WHERE client_id=destino;
 INSERT INTO redirect_uris(client_id,value) VALUES(destino,'http://localhost:4300/*');
 DELETE FROM web_origins WHERE client_id=destino;
 INSERT INTO web_origins(client_id,value) VALUES(destino,'http://localhost:4300');
END $$;
COMMIT;
