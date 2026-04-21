-- ============================================================
-- SCMS — Reset Script (Drop everything and start fresh)
-- File: sql/04_reset.sql
-- ============================================================
-- WARNING: This will DELETE all data and all objects.
-- Use only during development/testing.
--
-- Run as:
--   psql -U postgres -d studentdb -f sql/04_reset.sql
-- ============================================================

\c studentdb;

-- Drop stored procedure and function
DROP PROCEDURE IF EXISTS assign_grade(INT, INT, VARCHAR);
DROP FUNCTION  IF EXISTS get_student_avg(INT);

-- Drop tables (CASCADE handles foreign key dependencies)
DROP TABLE IF EXISTS enrollments CASCADE;
DROP TABLE IF EXISTS students    CASCADE;
DROP TABLE IF EXISTS courses     CASCADE;

DO $$
BEGIN
    RAISE NOTICE 'All SCMS objects dropped. Run 01_schema.sql to start fresh.';
END $$;
