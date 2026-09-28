-- Runs once on first start of the postgres container.
-- One database per service: services never touch each other's tables.
CREATE DATABASE auth_db;
CREATE DATABASE user_db;
CREATE DATABASE core_db;
CREATE DATABASE analytics_db;
CREATE DATABASE notification_db;
