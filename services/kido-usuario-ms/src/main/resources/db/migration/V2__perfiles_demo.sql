-- Cuentas de demostración del realm (infra/keycloak/kido-realm.json).
-- keycloak_id queda NULL: el primer GET /me de cada usuario lo enlaza por su username.
-- Los ids coinciden con los datos semilla: cursos del docente 1 e inscripción del estudiante 1001.
INSERT INTO perfiles (id, keycloak_id, username, email, nombres, apellidos, tipo_registro, docente_verificado, creado_en, actualizado_en) VALUES
    (1,    NULL, 'docente',    'docente@kido.local',    'Daniela', 'Rojas',  'DOCENTE',    TRUE,  now(), now()),
    (1001, NULL, 'estudiante', 'estudiante@kido.local', 'Mateo',   'Quispe', 'ESTUDIANTE', FALSE, now(), now()),
    (900,  NULL, 'admin',      'admin@kido.local',      'Equipo',  'KIDO',   'ESTUDIANTE', FALSE, now(), now());
