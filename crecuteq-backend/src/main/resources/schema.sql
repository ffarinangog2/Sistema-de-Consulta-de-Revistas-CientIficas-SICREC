-- INICIO - Auditoría general
-- El usuario es opcional para eventos donde no existe una autenticación
-- disponible, como intentos con usuarios inexistentes y exportaciones.
ALTER TABLE IF EXISTS auditoria
    ALTER COLUMN usuario_id DROP NOT NULL;

-- INICIO - Perfil Académico
CREATE TABLE IF NOT EXISTS perfil_academico (
    id_perfil_academico BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    google_scholar VARCHAR(500),
    orcid VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_perfil_academico_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_perfil_academico_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario)
);
-- FIN - Perfil Académico

-- INICIO - Foto de perfil
CREATE TABLE IF NOT EXISTS foto_perfil (
    id_foto_perfil BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    contenido BYTEA NOT NULL,
    tipo_contenido VARCHAR(50) NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_foto_perfil_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_foto_perfil_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);
-- FIN - Foto de perfil
-- FIN - Auditoría general

-- INICIO - Módulo Springer
CREATE TABLE IF NOT EXISTS springer_revista (
    id_springer_revista BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(500) NOT NULL,
    issn VARCHAR(20),
    issn_normalizado VARCHAR(8),
    eissn VARCHAR(20),
    eissn_normalizado VARCHAR(8),
    product_id VARCHAR(50),
    imprint VARCHAR(255),
    modelo_publicacion VARCHAR(100),
    tipo_hibrido VARCHAR(100),
    idioma_principal VARCHAR(100),
    url_oficial VARCHAR(1000),
    fuente_archivo VARCHAR(500),
    fecha_importacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_springer_revista_issn_normalizado
    ON springer_revista (issn_normalizado);

CREATE UNIQUE INDEX IF NOT EXISTS idx_springer_revista_eissn_normalizado
    ON springer_revista (eissn_normalizado);

CREATE UNIQUE INDEX IF NOT EXISTS idx_springer_revista_product_id
    ON springer_revista (LOWER(product_id));
-- FIN - Módulo Springer

-- INICIO - Importación PDF Springer
ALTER TABLE IF EXISTS springer_revista
    ADD COLUMN IF NOT EXISTS apc_eur VARCHAR(255),
    ADD COLUMN IF NOT EXISTS apc_usd VARCHAR(255),
    ADD COLUMN IF NOT EXISTS apc_gbp VARCHAR(255),
    ADD COLUMN IF NOT EXISTS apc_website VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS anio_vigencia_apc INTEGER;
-- FIN - Importación PDF Springer

-- INICIO - Datos editoriales del catálogo Springer
ALTER TABLE IF EXISTS springer_revista
    ADD COLUMN IF NOT EXISTS numeros_por_volumen INTEGER,
    ADD COLUMN IF NOT EXISTS numeros_programados INTEGER,
    ADD COLUMN IF NOT EXISTS comentarios VARCHAR(2000);

-- INICIO - Catálogo oficial DOAJ
CREATE TABLE IF NOT EXISTS doaj_revista (
    id_doaj_revista BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(1000) NOT NULL,
    issn VARCHAR(20),
    issn_normalizado VARCHAR(8),
    eissn VARCHAR(20),
    eissn_normalizado VARCHAR(8),
    editorial VARCHAR(1000),
    pais VARCHAR(255),
    idiomas VARCHAR(2000),
    materias VARCHAR(4000),
    licencia VARCHAR(1000),
    apc VARCHAR(255),
    moneda_apc VARCHAR(50),
    url_oficial VARCHAR(2000),
    datos_csv TEXT NOT NULL,
    fuente_archivo VARCHAR(500),
    fecha_importacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_doaj_issn_normalizado
    ON doaj_revista (issn_normalizado);
CREATE UNIQUE INDEX IF NOT EXISTS idx_doaj_eissn_normalizado
    ON doaj_revista (eissn_normalizado);

CREATE TABLE IF NOT EXISTS scopus_fuente (
    id_scopus_fuente BIGSERIAL PRIMARY KEY,
    issn VARCHAR(20),
    issn_normalizado VARCHAR(8),
    eissn VARCHAR(20),
    eissn_normalizado VARCHAR(8),
    estado VARCHAR(100),
    discontinuada BOOLEAN NOT NULL DEFAULT FALSE,
    periodicidad VARCHAR(255),
    fecha_importacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE IF EXISTS scopus_fuente
    ADD COLUMN IF NOT EXISTS discontinuada BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS idx_scopus_fuente_issn
    ON scopus_fuente (issn_normalizado);
CREATE UNIQUE INDEX IF NOT EXISTS idx_scopus_fuente_eissn
    ON scopus_fuente (eissn_normalizado);
CREATE TABLE IF NOT EXISTS elsevier_revista (
    id_elsevier_revista BIGSERIAL PRIMARY KEY,
    issn VARCHAR(20) NOT NULL,
    issn_normalizado VARCHAR(8) NOT NULL,
    titulo VARCHAR(1000) NOT NULL,
    modelo_publicacion VARCHAR(100),
    apc_usd VARCHAR(50), apc_eur VARCHAR(50),
    apc_gbp VARCHAR(50), apc_jpy VARCHAR(50),
    vigencia VARCHAR(100), fuente_archivo VARCHAR(500),
    fecha_importacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_elsevier_issn_normalizado
    ON elsevier_revista (issn_normalizado);
CREATE TABLE IF NOT EXISTS wiley_revista (
    id_wiley_revista BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(1000) NOT NULL,
    online_issn VARCHAR(20) NOT NULL,
    online_issn_normalizado VARCHAR(8) NOT NULL,
    area_tematica VARCHAR(1000),
    licencias VARCHAR(1000),
    modelo_publicacion VARCHAR(100) NOT NULL,
    apc_usd VARCHAR(50), apc_gbp VARCHAR(50), apc_eur VARCHAR(50),
    vigencia VARCHAR(100), fuente_archivo VARCHAR(500),
    fecha_importacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_wiley_online_issn
    ON wiley_revista (online_issn_normalizado);
CREATE TABLE IF NOT EXISTS sage_revista (
    id_sage_revista BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(1000) NOT NULL,
    journal_code VARCHAR(50) NOT NULL,
    tla VARCHAR(20), issn VARCHAR(20), issn_normalizado VARCHAR(8),
    eissn VARCHAR(20), eissn_normalizado VARCHAR(8),
    modelo_publicacion VARCHAR(50) NOT NULL,
    division VARCHAR(50), precio_lista VARCHAR(50), precio_actual VARCHAR(50),
    moneda VARCHAR(20), apc_usd VARCHAR(50), apc_gbp VARCHAR(50),
    url_oficial VARCHAR(2000), fuente_archivo VARCHAR(500),
    fecha_importacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sage_modelo_codigo UNIQUE (modelo_publicacion, journal_code)
);
CREATE INDEX IF NOT EXISTS idx_sage_issn ON sage_revista (issn_normalizado);
CREATE INDEX IF NOT EXISTS idx_sage_eissn ON sage_revista (eissn_normalizado);
CREATE TABLE IF NOT EXISTS cambridge_revista (
    id_cambridge_revista BIGSERIAL PRIMARY KEY,
    mnemonic VARCHAR(30) NOT NULL UNIQUE, titulo VARCHAR(1000) NOT NULL,
    issn VARCHAR(20), issn_normalizado VARCHAR(8), eissn VARCHAR(20), eissn_normalizado VARCHAR(8),
    area VARCHAR(20), modelo_publicacion VARCHAR(100),
    apc_gbp VARCHAR(50), apc_gbp_miembro VARCHAR(50), apc_usd VARCHAR(50), apc_usd_miembro VARCHAR(50),
    apc_eur VARCHAR(50), apc_eur_miembro VARCHAR(50), apc_aud VARCHAR(50), apc_aud_miembro VARCHAR(50),
    notas_apc VARCHAR(2000), licencias VARCHAR(1000), url_oficial VARCHAR(2000), fuente_archivo VARCHAR(500),
    fecha_importacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cambridge_issn ON cambridge_revista (issn_normalizado);
CREATE INDEX IF NOT EXISTS idx_cambridge_eissn ON cambridge_revista (eissn_normalizado);
CREATE TABLE IF NOT EXISTS ieee_revista (
    id_ieee_revista BIGSERIAL PRIMARY KEY,
    acronimo VARCHAR(40) NOT NULL UNIQUE, titulo VARCHAR(1000) NOT NULL,
    issn VARCHAR(20), issn_normalizado VARCHAR(8), eissn VARCHAR(20), eissn_normalizado VARCHAR(8),
    tipo_acceso VARCHAR(80), apc_usd VARCHAR(50), cargo_sobreextension VARCHAR(100),
    tarifa_licencia_repositorio VARCHAR(50), url_oficial VARCHAR(2000), fuente_archivo VARCHAR(500),
    fecha_importacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ieee_issn ON ieee_revista (issn_normalizado);
CREATE INDEX IF NOT EXISTS idx_ieee_eissn ON ieee_revista (eissn_normalizado);
CREATE TABLE IF NOT EXISTS degruyter_revista (
    id_degruyter_revista BIGSERIAL PRIMARY KEY,
    codigo_online VARCHAR(80) NOT NULL UNIQUE, titulo VARCHAR(1000) NOT NULL,
    issn VARCHAR(20), issn_normalizado VARCHAR(8), eissn VARCHAR(20), eissn_normalizado VARCHAR(8),
    editorial VARCHAR(255), modelo_publicacion VARCHAR(100), apc_eur VARCHAR(255),
    area_tematica VARCHAR(500), idioma VARCHAR(200), licencia VARCHAR(500),
    url_oficial VARCHAR(2000), fuente_archivo VARCHAR(500),
    fecha_importacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE degruyter_revista ALTER COLUMN apc_eur TYPE VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_degruyter_issn ON degruyter_revista (issn_normalizado);
CREATE INDEX IF NOT EXISTS idx_degruyter_eissn ON degruyter_revista (eissn_normalizado);
CREATE TABLE IF NOT EXISTS brill_revista (
    id_brill_revista BIGSERIAL PRIMARY KEY,
    eissn_normalizado VARCHAR(8) NOT NULL UNIQUE, eissn VARCHAR(20) NOT NULL,
    titulo VARCHAR(1000) NOT NULL, editorial VARCHAR(100), apc_eur VARCHAR(50),
    tipo_oa VARCHAR(100), area_tematica VARCHAR(500), subdisciplina VARCHAR(500),
    factor_impacto VARCHAR(50), idioma VARCHAR(200), url_oficial VARCHAR(2000),
    fuente_archivo VARCHAR(500), fecha_importacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_brill_eissn ON brill_revista (eissn_normalizado);
-- FIN - Catálogo oficial DOAJ
-- FIN - Datos editoriales del catálogo Springer

-- INICIO - Equivalencias Facultad UTEQ / clasificación ASJC de Scopus
CREATE TABLE IF NOT EXISTS facultad (
    facultad_codigo VARCHAR(20) PRIMARY KEY,
    facultad_nombre VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO facultad (facultad_codigo, facultad_nombre) VALUES
    ('FCE', 'Ciencias Empresariales'),
    ('FCCD', 'Ciencias de la Computación y Diseño Digital'),
    ('FCED', 'Ciencias de la Educación'),
    ('FCSEF', 'Ciencias Sociales, Económicas y Financieras'),
    ('FCS', 'Ciencias de la Salud'),
    ('FCI', 'Ciencias de la Ingeniería'),
    ('FCAF', 'Ciencias Agrarias y Forestales'),
    ('FCIP', 'Ciencias de la Industria y Producción'),
    ('FCPB', 'Ciencias Pecuarias y Biológicas')
ON CONFLICT (facultad_codigo)
DO UPDATE SET facultad_nombre = EXCLUDED.facultad_nombre;

CREATE TABLE IF NOT EXISTS facultad_scopus_area (
    facultad_codigo VARCHAR(20) NOT NULL,
    subarea_codigo VARCHAR(4) NOT NULL,
    CONSTRAINT facultad_scopus_area_pkey PRIMARY KEY (facultad_codigo, subarea_codigo)
);

-- Migración idempotente de instalaciones creadas con la estructura descriptiva.
-- Los nombres y abreviaturas oficiales se obtienen de subject-area en Scopus.
ALTER TABLE facultad_scopus_area DROP CONSTRAINT IF EXISTS uk_facultad_scopus_area;
ALTER TABLE facultad_scopus_area DROP CONSTRAINT IF EXISTS facultad_scopus_area_pkey;
ALTER TABLE facultad_scopus_area
    DROP COLUMN IF EXISTS id_facultad_scopus_area,
    DROP COLUMN IF EXISTS facultad_nombre,
    DROP COLUMN IF EXISTS area_codigo,
    DROP COLUMN IF EXISTS area_nombre,
    DROP COLUMN IF EXISTS subarea_nombre;
ALTER TABLE facultad_scopus_area
    ADD CONSTRAINT facultad_scopus_area_pkey PRIMARY KEY (facultad_codigo, subarea_codigo);
ALTER TABLE facultad_scopus_area
    DROP CONSTRAINT IF EXISTS fk_facultad_scopus_area_facultad;
ALTER TABLE facultad_scopus_area
    ADD CONSTRAINT fk_facultad_scopus_area_facultad
        FOREIGN KEY (facultad_codigo)
        REFERENCES facultad (facultad_codigo);

-- Catálogo oficial completo de la hoja "ASJC Classification Codes" del
-- Source Title List de Scopus (junio de 2026): código multidisciplinario 1000
-- y 333 códigos generales/específicos distribuidos entre las áreas 11 a 36.
-- La segunda relación es institucional: una familia ASJC puede corresponder a
-- varias facultades, sin modificar la clasificación que devuelve Scopus.
WITH asjc_rango(area_prefijo, ultimo_subcodigo) AS (
    VALUES
    (11, 11), (12, 13), (13, 15), (14, 10), (15, 8), (16, 7),
    (17, 12), (18, 4), (19, 13), (20, 3), (21, 5), (22, 16),
    (23, 12), (24, 6), (25, 8), (26, 14), (27, 48), (28, 9),
    (29, 23), (30, 5), (31, 10), (32, 7), (33, 22), (34, 4),
    (35, 6), (36, 16)
),
asjc_oficial(codigo, area_prefijo) AS (
    SELECT '1000', 10
    UNION ALL
    SELECT area_prefijo::text || LPAD(subcodigo::text, 2, '0'), area_prefijo
    FROM asjc_rango
    CROSS JOIN LATERAL generate_series(0, ultimo_subcodigo) AS subcodigo
),
facultad_area(facultad_codigo, area_prefijo) AS (
    VALUES
    ('FCAF', 11), ('FCPB', 11), ('FCIP', 11), ('FCS', 11),
    ('FCED', 12), ('FCSEF', 12), ('FCCD', 12),
    ('FCPB', 13), ('FCS', 13), ('FCAF', 13), ('FCIP', 13),
    ('FCE', 14), ('FCSEF', 14), ('FCIP', 14),
    ('FCI', 15), ('FCIP', 15), ('FCAF', 15),
    ('FCI', 16), ('FCIP', 16), ('FCAF', 16), ('FCPB', 16), ('FCS', 16),
    ('FCCD', 17), ('FCI', 17), ('FCIP', 17), ('FCE', 17), ('FCED', 17),
    ('FCE', 18), ('FCSEF', 18), ('FCI', 18), ('FCIP', 18), ('FCCD', 18),
    ('FCI', 19), ('FCAF', 19), ('FCPB', 19), ('FCIP', 19),
    ('FCE', 20), ('FCSEF', 20),
    ('FCI', 21), ('FCIP', 21), ('FCAF', 21),
    ('FCI', 22), ('FCIP', 22), ('FCCD', 22),
    ('FCAF', 23), ('FCPB', 23), ('FCI', 23), ('FCIP', 23), ('FCS', 23), ('FCSEF', 23),
    ('FCPB', 24), ('FCS', 24), ('FCAF', 24), ('FCIP', 24),
    ('FCI', 25), ('FCIP', 25), ('FCCD', 25),
    ('FCI', 26), ('FCCD', 26), ('FCE', 26), ('FCSEF', 26), ('FCED', 26), ('FCIP', 26),
    ('FCS', 27), ('FCPB', 27), ('FCED', 27),
    ('FCS', 28), ('FCPB', 28), ('FCED', 28), ('FCCD', 28),
    ('FCS', 29), ('FCED', 29),
    ('FCS', 30), ('FCPB', 30), ('FCIP', 30), ('FCAF', 30),
    ('FCI', 31), ('FCIP', 31), ('FCCD', 31), ('FCED', 31),
    ('FCS', 32), ('FCED', 32), ('FCSEF', 32), ('FCCD', 32),
    ('FCSEF', 33), ('FCED', 33), ('FCE', 33), ('FCCD', 33), ('FCS', 33),
    ('FCPB', 34), ('FCAF', 34), ('FCS', 34),
    ('FCS', 35),
    ('FCS', 36), ('FCED', 36)
),
facultades(facultad_codigo) AS (
    VALUES ('FCE'), ('FCCD'), ('FCED'), ('FCSEF'), ('FCS'),
           ('FCI'), ('FCAF'), ('FCIP'), ('FCPB')
),
equivalencias AS (
    SELECT facultad_codigo, '1000' AS subarea_codigo FROM facultades
    UNION
    SELECT fa.facultad_codigo, ao.codigo
    FROM facultad_area fa
    JOIN asjc_oficial ao ON ao.area_prefijo = fa.area_prefijo
)
INSERT INTO facultad_scopus_area (facultad_codigo, subarea_codigo)
SELECT facultad_codigo, subarea_codigo
FROM equivalencias
ON CONFLICT (facultad_codigo, subarea_codigo) DO NOTHING;
-- FIN - Equivalencias Facultad UTEQ / clasificación ASJC de Scopus
-- INICIO - Migracion idempotente de roles y cargos
INSERT INTO rol (nombre_rol) VALUES ('ADMIN'), ('USUARIO')
ON CONFLICT (nombre_rol) DO NOTHING;
INSERT INTO cargo (nombre_cargo) VALUES ('Docente'), ('Estudiante'), ('Personal de Investigación')
ON CONFLICT (nombre_cargo) DO NOTHING;
UPDATE usuario u SET
    cargo_id = CASE r.nombre_rol
        WHEN 'DOCENTE' THEN (SELECT id_cargo FROM cargo WHERE nombre_cargo = 'Docente')
        WHEN 'ESTUDIANTE' THEN (SELECT id_cargo FROM cargo WHERE nombre_cargo = 'Estudiante')
        ELSE (SELECT id_cargo FROM cargo WHERE nombre_cargo = 'Personal de Investigación')
    END,
    rol_id = (SELECT id_rol FROM rol WHERE nombre_rol = 'USUARIO')
FROM rol r
WHERE u.rol_id = r.id_rol
  AND r.nombre_rol IN ('DOCENTE', 'ESTUDIANTE', 'PERSONAL_INVESTIGACION', 'PERSONAL_DE_INVESTIGACION');
DELETE FROM rol r
WHERE r.nombre_rol NOT IN ('ADMIN', 'USUARIO')
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.rol_id = r.id_rol);
-- FIN - Migracion idempotente de roles y cargos
