-- noinspection SqlNoDataSourceInspectionForFile

-- jaxrsdb
CREATE USER jamesbond WITH PASSWORD 's3cr3t';
CREATE DATABASE jaxrsdb;
GRANT ALL PRIVILEGES ON DATABASE jaxrsdb TO jamesbond;
ALTER ROLE jamesbond SUPERUSER;
