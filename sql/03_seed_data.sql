-- ============================================================
-- SCMS — Sample Seed Data
-- File: sql/03_seed_data.sql
-- ============================================================
-- Run AFTER 01_schema.sql and 02_procedures.sql:
--   psql -U postgres -d studentdb -f sql/03_seed_data.sql
--
-- This populates the database with demo data so you can
-- explore the application immediately.
-- ============================================================

\c studentdb;

-- Clear existing data (useful for re-seeding during development)
TRUNCATE TABLE enrollments RESTART IDENTITY CASCADE;
TRUNCATE TABLE students    RESTART IDENTITY CASCADE;
TRUNCATE TABLE courses     RESTART IDENTITY CASCADE;

-- ─────────────────────────────────────────────────────────────
-- Students
-- ─────────────────────────────────────────────────────────────
INSERT INTO students (name, email, dob) VALUES
    ('Arjun Sharma',     'arjun.sharma@example.com',   '2001-03-15'),
    ('Priya Nair',       'priya.nair@example.com',     '2000-07-22'),
    ('Rahul Gupta',      'rahul.gupta@example.com',    '2002-01-08'),
    ('Ananya Singh',     'ananya.singh@example.com',   '2001-11-30'),
    ('Vikram Mehta',     'vikram.mehta@example.com',   '2000-05-17'),
    ('Deepika Pillai',   'deepika.pillai@example.com', '2002-09-05'),
    ('Karan Patel',      'karan.patel@example.com',    '2001-02-28'),
    ('Sneha Reddy',      'sneha.reddy@example.com',    '2000-12-10');

-- ─────────────────────────────────────────────────────────────
-- Courses
-- ─────────────────────────────────────────────────────────────
INSERT INTO courses (name, credits) VALUES
    ('Introduction to Computer Science',  3),
    ('Data Structures and Algorithms',    4),
    ('Database Management Systems',       3),
    ('Operating Systems',                 3),
    ('Web Development Fundamentals',      2),
    ('Machine Learning Basics',           4),
    ('Software Engineering Principles',   3),
    ('Computer Networks',                 3);

-- ─────────────────────────────────────────────────────────────
-- Enrollments (with grades)
-- ─────────────────────────────────────────────────────────────
INSERT INTO enrollments (student_id, course_id, grade) VALUES
    -- Arjun (student 1)
    (1, 1, 'A'),
    (1, 2, 'B+'),
    (1, 3, 'A-'),
    -- Priya (student 2)
    (2, 1, 'A+'),
    (2, 4, 'B'),
    (2, 5, 'A'),
    -- Rahul (student 3)
    (3, 2, 'C+'),
    (3, 3, 'B'),
    -- Ananya (student 4)
    (4, 6, 'A'),
    (4, 7, 'A-'),
    (4, 1, NULL),   -- enrolled but not yet graded
    -- Vikram (student 5)
    (5, 4, 'B-'),
    (5, 8, 'C'),
    -- Deepika (student 6)
    (6, 5, 'A+'),
    (6, 6, 'B+'),
    -- Karan (student 7)
    (7, 1, 'B'),
    (7, 3, 'B+'),
    (7, 7, NULL),   -- enrolled but not yet graded
    -- Sneha (student 8)
    (8, 2, 'A-'),
    (8, 6, 'A'),
    (8, 8, 'B+');

-- ─────────────────────────────────────────────────────────────
-- Confirmation
-- ─────────────────────────────────────────────────────────────
SELECT 'Students inserted: ' || COUNT(*) FROM students;
SELECT 'Courses inserted: '  || COUNT(*) FROM courses;
SELECT 'Enrollments inserted: ' || COUNT(*) FROM enrollments;
