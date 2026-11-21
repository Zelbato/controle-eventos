-- Execute como superusuário (postgres) no pgAdmin ou psql, caso NÃO use o Docker.
-- As tabelas são criadas automaticamente pelo Hibernate (ddl-auto=update).
-- Se o seu PostgreSQL estiver na porta padrão 5432, ajuste spring.datasource.url
-- no application.properties ou defina a variável DB_URL.

CREATE USER eventos_app WITH PASSWORD 'eventos_local_2026';
CREATE DATABASE controle_eventos OWNER eventos_app;
