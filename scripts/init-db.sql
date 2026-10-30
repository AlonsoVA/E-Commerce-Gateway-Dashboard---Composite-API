-- Base local para la evidencia. Se puede ejecutar más de una vez.
-- psql -h 127.0.0.1 -d postgres -f scripts/init-db.sql

SELECT format('CREATE ROLE ecommerce LOGIN PASSWORD %L', 'ecommerce')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ecommerce')\gexec

SELECT 'CREATE DATABASE ecommerce_gateway OWNER ecommerce'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ecommerce_gateway')\gexec

\connect ecommerce_gateway

CREATE SCHEMA IF NOT EXISTS catalog AUTHORIZATION ecommerce;
CREATE SCHEMA IF NOT EXISTS inventory AUTHORIZATION ecommerce;
