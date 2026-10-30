CREATE TABLE intentos_checkout_interno (
 referencia VARCHAR(80) PRIMARY KEY,
 clave VARCHAR(36) NOT NULL,
 huella VARCHAR(64) NOT NULL,
 estado VARCHAR(30) NOT NULL,
 payment_id VARCHAR(40),
 actualizado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
