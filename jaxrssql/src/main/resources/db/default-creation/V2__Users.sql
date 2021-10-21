-- User used by flyway to migrate the database used by the application
CREATE USER ${migration.user};
ALTER USER ${migration.user} WITH ENCRYPTED PASSWORD '${migration.password}';
GRANT ALL PRIVILEGES ON DATABASE ${migration.database} TO ${migration.user};

-- User used by application to read and write data
CREATE USER ${app.user};
ALTER USER ${app.user} WITH ENCRYPTED PASSWORD '${app.password}';
GRANT CONNECT ON DATABASE ${app.database} to ${app.user};
