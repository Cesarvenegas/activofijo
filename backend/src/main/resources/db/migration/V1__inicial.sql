-- Migración inicial (Etapa 0).
-- Solo deja registrada la versión del esquema; las tablas de negocio llegan desde la Etapa 1.
CREATE TABLE parametro_sistema (
    clave      varchar(100) PRIMARY KEY,
    valor      varchar(500) NOT NULL,
    creado_en  timestamptz  NOT NULL DEFAULT now()
);

INSERT INTO parametro_sistema (clave, valor) VALUES ('esquema.etapa', '0');
