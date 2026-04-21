-- ============================================================
-- SCMS — Database Schema Setup
-- File: sql/01_schema.sql
-- ============================================================
-- Run this script ONCE to create the database and tables.
--
-- How to run:
--   psql -U postgres -f sql/01_schema.sql
-- ============================================================

-- Step 1: Create the database (run as superuser)
-- If 'studentdb' already exists, this will error — that's OK.
-- You can comment this line out and just run the rest.
CREATE DATABASE studentdb;

-- Step 2: Connect to the new database
\c studentdb;

-- ─────────────────────────────────────────────────────────────
-- TABLE: students
-- ─────────────────────────────────────────────────────────────
-- Stores registered students.
-- 'id' uses SERIAL — PostgreSQL auto-increment type.
-- 'email' is UNIQUE to prevent duplicate registrations.

CREATE TABLE IF NOT EXISTS students (
    id    SERIAL PRIMARY KEY,
    name  VARCHAR(100)        NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    dob   DATE                NOT NULL
);

-- ─────────────────────────────────────────────────────────────
-- TABLE: courses
-- ─────────────────────────────────────────────────────────────
-- Stores available courses.
-- 'credits' is a small integer (1–6 typical for a course).

CREATE TABLE IF NOT EXISTS courses (
    id      SERIAL PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    credits INT          NOT NULL CHECK (credits BETWEEN 1 AND 6)
);

-- ─────────────────────────────────────────────────────────────
-- TABLE: enrollments
-- ─────────────────────────────────────────────────────────────
-- Represents a student enrolled in a course.
-- Foreign keys ensure referential integrity:
--   - student_id must exist in students
--   - course_id must exist in courses
-- UNIQUE(student_id, course_id) prevents double-enrollment.
-- grade is nullable — set later by admin via assign_grade().

CREATE TABLE IF NOT EXISTS enrollments (
    id         SERIAL PRIMARY KEY,
    student_id INT         NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    course_id  INT         NOT NULL REFERENCES courses(id)  ON DELETE CASCADE,
    grade      VARCHAR(3)  NULL,       -- e.g., 'A', 'B+', 'C-'
    UNIQUE(student_id, course_id)      -- A student can enroll in a course only once
);

-- ─────────────────────────────────────────────────────────────
-- INDEXES for performance
-- ─────────────────────────────────────────────────────────────
-- Speed up lookups by student and course in the enrollments table.

CREATE INDEX IF NOT EXISTS idx_enrollments_student ON enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_course  ON enrollments(course_id);

-- ─────────────────────────────────────────────────────────────
-- Confirmation
-- ─────────────────────────────────────────────────────────────
DO $$
BEGIN
    RAISE NOTICE 'Schema created successfully: students, courses, enrollments';
END $$;
