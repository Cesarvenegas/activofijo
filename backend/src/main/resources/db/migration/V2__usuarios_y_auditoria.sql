-- Etapa 1: usuarios, eventos de auditoría y sesiones (Spring Session JDBC).
-- Sin multiempresa por ahora: se agregará empresa_id en una etapa posterior.

CREATE TABLE usuario (
    id                     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rut                    varchar(12)  NOT NULL,      -- RUN normalizado: 12345678-K (es el login)
    nombre                 varchar(150) NOT NULL,
    email                  varchar(150),
    telefono_anexo         varchar(30),
    cargo                  varchar(100),
    password_hash          varchar(100) NOT NULL,      -- BCrypt
    rol                    varchar(20)  NOT NULL,
    activo                 boolean      NOT NULL DEFAULT true,
    debe_cambiar_password  boolean      NOT NULL DEFAULT true,
    ultimo_acceso          timestamptz,
    intentos_fallidos      int          NOT NULL DEFAULT 0,
    bloqueado_hasta        timestamptz,
    creado_en              timestamptz  NOT NULL DEFAULT now(),
    creado_por             varchar(12),
    modificado_en          timestamptz,
    modificado_por         varchar(12),
    CONSTRAINT uk_usuario_rut UNIQUE (rut),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMIN', 'OPERADOR', 'CONSULTA'))
);

CREATE INDEX ix_usuario_nombre ON usuario (lower(nombre));

CREATE TABLE evento_auditoria (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo        varchar(40)  NOT NULL,
    usuario_id  bigint REFERENCES usuario (id),
    rut         varchar(12),                 -- RUN ingresado (útil en logins fallidos de RUN inexistente)
    detalle     varchar(500),
    ip          varchar(45),
    fecha       timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX ix_evento_fecha ON evento_auditoria (fecha DESC);
CREATE INDEX ix_evento_usuario ON evento_auditoria (usuario_id, fecha DESC);

-- Esquema oficial de Spring Session JDBC 4.1 para PostgreSQL
CREATE TABLE spring_session (
    primary_id             char(36)     NOT NULL,
    session_id             char(36)     NOT NULL,
    creation_time          bigint       NOT NULL,
    last_access_time       bigint       NOT NULL,
    max_inactive_interval  int          NOT NULL,
    expiry_time            bigint       NOT NULL,
    principal_name         varchar(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id  char(36)      NOT NULL,
    attribute_name      varchar(200)  NOT NULL,
    attribute_bytes     bytea         NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session (primary_id) ON DELETE CASCADE
);

UPDATE parametro_sistema SET valor = '1' WHERE clave = 'esquema.etapa';
