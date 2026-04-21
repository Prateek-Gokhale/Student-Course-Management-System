-- ============================================================
-- SCMS — Stored Procedures & Functions
-- File: sql/02_procedures.sql
-- ============================================================
-- Run AFTER 01_schema.sql:
--   psql -U postgres -d studentdb -f sql/02_procedures.sql
-- ============================================================

\c studentdb;

-- ─────────────────────────────────────────────────────────────
-- STORED PROCEDURE: assign_grade
-- ─────────────────────────────────────────────────────────────
-- Called from Java via CallableStatement.
--
-- Purpose: Assign or update a grade for a student in a course.
-- If the enrollment doesn't exist, raises a meaningful error.
--
-- Usage from Java:
--   CallableStatement cs = conn.prepareCall("{ CALL assign_grade(?, ?, ?) }");
--   cs.setInt(1, studentId);
--   cs.setInt(2, courseId);
--   cs.setString(3, "A+");
--   cs.execute();

CREATE OR REPLACE PROCEDURE assign_grade(
    p_student_id INT,
    p_course_id  INT,
    p_grade      VARCHAR(3)
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_count INT;
BEGIN
    -- Verify the enrollment exists
    SELECT COUNT(*) INTO v_count
    FROM enrollments
    WHERE student_id = p_student_id
      AND course_id  = p_course_id;

    IF v_count = 0 THEN
        RAISE EXCEPTION 'No enrollment found for student_id=% and course_id=%',
            p_student_id, p_course_id;
    END IF;

    -- Update the grade
    UPDATE enrollments
    SET grade = UPPER(p_grade)
    WHERE student_id = p_student_id
      AND course_id  = p_course_id;

    RAISE NOTICE 'Grade % assigned to student % for course %', p_grade, p_student_id, p_course_id;
END;
$$;


-- ─────────────────────────────────────────────────────────────
-- STORED FUNCTION: get_student_avg
-- ─────────────────────────────────────────────────────────────
-- Called from Java via CallableStatement with a return value.
--
-- Purpose: Calculate a student's average GPA based on their grades.
-- Returns a NUMERIC(4,2) value on a 0.00–4.00 scale.
-- Returns 0.00 if the student has no graded enrollments.
--
-- Grade → GPA mapping:
--   A+/A = 4.0, A- = 3.7
--   B+   = 3.3, B  = 3.0, B- = 2.7
--   C+   = 2.3, C  = 2.0, C- = 1.7
--   D    = 1.0, F  = 0.0
--
-- Usage from Java:
--   CallableStatement cs = conn.prepareCall("{ ? = CALL get_student_avg(?) }");
--   cs.registerOutParameter(1, Types.NUMERIC);
--   cs.setInt(2, studentId);
--   cs.execute();
--   double gpa = cs.getDouble(1);

CREATE OR REPLACE FUNCTION get_student_avg(p_student_id INT)
RETURNS NUMERIC(4, 2)
LANGUAGE plpgsql
AS $$
DECLARE
    v_avg     NUMERIC(4, 2);
    v_total   NUMERIC := 0;
    v_count   INT     := 0;
    v_points  NUMERIC;
    v_grade   VARCHAR(3);
BEGIN
    -- Loop through all graded enrollments for this student
    FOR v_grade IN
        SELECT grade
        FROM enrollments
        WHERE student_id = p_student_id
          AND grade IS NOT NULL
    LOOP
        -- Convert letter grade to grade points
        v_points := CASE UPPER(v_grade)
            WHEN 'A+'  THEN 4.0
            WHEN 'A'   THEN 4.0
            WHEN 'A-'  THEN 3.7
            WHEN 'B+'  THEN 3.3
            WHEN 'B'   THEN 3.0
            WHEN 'B-'  THEN 2.7
            WHEN 'C+'  THEN 2.3
            WHEN 'C'   THEN 2.0
            WHEN 'C-'  THEN 1.7
            WHEN 'D'   THEN 1.0
            WHEN 'F'   THEN 0.0
            ELSE 0.0
        END;

        v_total := v_total + v_points;
        v_count := v_count + 1;
    END LOOP;

    -- Avoid division by zero
    IF v_count = 0 THEN
        RETURN 0.00;
    END IF;

    v_avg := v_total / v_count;
    RETURN ROUND(v_avg, 2);
END;
$$;


-- ─────────────────────────────────────────────────────────────
-- Confirmation
-- ─────────────────────────────────────────────────────────────
DO $$
BEGIN
    RAISE NOTICE 'Stored procedure assign_grade() created.';
    RAISE NOTICE 'Stored function get_student_avg() created.';
END $$;
