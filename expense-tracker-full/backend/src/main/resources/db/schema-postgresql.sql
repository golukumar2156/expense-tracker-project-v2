-- PostgreSQL reference schema for the assignment submission.
-- The runnable Spring Boot configuration uses MySQL (see backend/application.properties).
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE expense_categories (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(60) NOT NULL,
  description VARCHAR(180),
  user_id BIGINT NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_category_user_name UNIQUE(user_id, name)
);
CREATE TABLE expenses (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
  expense_date DATE NOT NULL,
  user_id BIGINT NOT NULL REFERENCES users(id),
  category_id BIGINT NOT NULL REFERENCES expense_categories(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_expense_user_date ON expenses(user_id, expense_date);
CREATE INDEX idx_expense_category ON expenses(category_id);
