-- Fix column type mismatch: CHAR(3) -> VARCHAR(3) to match JPA entity mapping.
ALTER TABLE payments ALTER COLUMN currency TYPE VARCHAR(3);
