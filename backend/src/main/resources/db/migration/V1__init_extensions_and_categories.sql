-- Extensions used across the platform (section 5 of the architecture doc).
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- taxonomy module: problem categories, English + Hindi names for the Lucknow pilot.
CREATE TABLE categories (
    id          UUID PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name_en     VARCHAR(120) NOT NULL,
    name_hi     VARCHAR(120) NOT NULL,
    parent_id   UUID REFERENCES categories (id),
    sort_order  INT          NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO categories (id, code, name_en, name_hi, sort_order) VALUES
    ('0193f1a0-0000-7000-8000-000000000001', 'ROADS_INFRASTRUCTURE', 'Roads and infrastructure', 'सड़क और बुनियादी ढांचा', 10),
    ('0193f1a0-0000-7000-8000-000000000002', 'DRAINAGE_WATER',       'Drainage and water',       'जल निकासी और पानी',      20),
    ('0193f1a0-0000-7000-8000-000000000003', 'WASTE_MANAGEMENT',     'Waste management',         'कचरा प्रबंधन',           30),
    ('0193f1a0-0000-7000-8000-000000000004', 'PUBLIC_TRANSPORT',     'Public transportation',    'सार्वजनिक परिवहन',       40),
    ('0193f1a0-0000-7000-8000-000000000005', 'EDUCATION',            'Education',                'शिक्षा',                 50),
    ('0193f1a0-0000-7000-8000-000000000006', 'HEALTHCARE',           'Healthcare',               'स्वास्थ्य सेवा',          60),
    ('0193f1a0-0000-7000-8000-000000000007', 'ENVIRONMENT',          'Environment',              'पर्यावरण',               70),
    ('0193f1a0-0000-7000-8000-000000000008', 'LOCAL_EMPLOYMENT',     'Local employment',         'स्थानीय रोजगार',         80);
