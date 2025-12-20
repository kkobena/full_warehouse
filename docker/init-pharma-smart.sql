-- init-pharma-smart.sql
CREATE DATABASE pharma_smart;

\connect pharma_smart;

CREATE ROLE pharma_smart LOGIN PASSWORD '2802_pharma_smart';
ALTER DATABASE pharma_smart OWNER TO pharma_smart;

CREATE SCHEMA IF NOT EXISTS pharma_smart AUTHORIZATION pharma_smart;

GRANT ALL PRIVILEGES ON SCHEMA pharma_smart TO pharma_smart;
ALTER DEFAULT PRIVILEGES IN SCHEMA pharma_smart GRANT ALL ON TABLES TO pharma_smart;
ALTER DEFAULT PRIVILEGES IN SCHEMA pharma_smart GRANT ALL ON SEQUENCES TO pharma_smart;
