-- Esquema de la API de recetas: DDL de docs/ra3.md (sección 2).

CREATE TABLE vademecum (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    composicion TEXT,
    funcion VARCHAR(255),
    presentacion VARCHAR(100),
    dosificacion VARCHAR(100),
    casa_comercial VARCHAR(100),
    contraindicaciones TEXT
);

CREATE TABLE cie10 (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    categoria VARCHAR(100),
    CONSTRAINT uk_cie10_codigo UNIQUE (codigo)
);

CREATE TABLE vademecum_cie10 (
    id_vademecum INT NOT NULL,
    id_cie10 INT NOT NULL,
    CONSTRAINT pk_vademecum_cie10 PRIMARY KEY (id_vademecum, id_cie10),
    CONSTRAINT fk_vademecum_cie10_vademecum FOREIGN KEY (id_vademecum) REFERENCES vademecum (id) ON DELETE CASCADE,
    CONSTRAINT fk_vademecum_cie10_cie10 FOREIGN KEY (id_cie10) REFERENCES cie10 (id) ON DELETE CASCADE
);
