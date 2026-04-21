package com.scms.dao;

import com.scms.model.Enrollment;
import com.scms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * EnrollmentDAO — Data Access Object for Enrollments
 * ============================================================
 *
 * Demonstrates:
 *  - Transaction management (commit / rollback)
 *  - Savepoints (partial rollback)
 *  - CallableStatement (stored procedure: assign_grade)
 *  - CallableStatement (function: get_student_avg)
 */
public class EnrollmentDAO {

    // ─────────────────────────────────────────────────────────────
    // ENROLL STUDENT — with Transaction + Savepoint
    // ─────────────────────────────────────────────────────────────

    /**
     * Transaction Demo:
     *
     * Enrolling a student involves multiple steps:
     *  1. Check the student exists
     *  2. Check the course exists
     *  3. Insert the enrollment record
     *
     * All steps must succeed or none should be saved.
     *
     * Savepoint: After step 1 (validations), we set a savepoint.
     * If step 3 fails, we can roll back ONLY the insert,
     * not the entire transaction.
     */
    public Enrollment enrollStudentWithTransaction(int studentId, int courseId) throws SQLException {
        Connection conn = DBConnection.getPooledConnection();

        try {
            // ── Disable auto-commit: we control when to commit ────
            conn.setAutoCommit(false);

            // ── Step 1: Validate student exists ───────────────────
            String checkStudent = "SELECT id FROM students WHERE id = ?";
            PreparedStatement psStudent = conn.prepareStatement(checkStudent);
            psStudent.setInt(1, studentId);
            ResultSet rsS = psStudent.executeQuery();
            if (!rsS.next()) {
                conn.rollback(); // Undo everything
                throw new SQLException("Student with ID " + studentId + " does not exist.");
            }
            psStudent.close();

            // ── Step 2: Validate course exists ────────────────────
            String checkCourse = "SELECT id FROM courses WHERE id = ?";
            PreparedStatement psCourse = conn.prepareStatement(checkCourse);
            psCourse.setInt(1, courseId);
            ResultSet rsC = psCourse.executeQuery();
            if (!rsC.next()) {
                conn.rollback();
                throw new SQLException("Course with ID " + courseId + " does not exist.");
            }
            psCourse.close();

            // ── SAVEPOINT: validations passed ─────────────────────
            // From here, if only the INSERT fails, we can rollback
            // to this point without losing the validation checks.
            Savepoint savepoint = conn.setSavepoint("AFTER_VALIDATION");

            // ── Step 3: Insert enrollment ─────────────────────────
            String insertSql = "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?) RETURNING id";
            PreparedStatement psInsert = conn.prepareStatement(insertSql);
            psInsert.setInt(1, studentId);
            psInsert.setInt(2, courseId);

            Enrollment enrollment = null;
            try {
                ResultSet rs = psInsert.executeQuery();
                if (rs.next()) {
                    enrollment = new Enrollment(rs.getInt("id"), studentId, courseId, null);
                }
            } catch (SQLException e) {
                // Only roll back to the savepoint (not the whole transaction)
                conn.rollback(savepoint);
                conn.commit();
                throw new SQLException("Enrollment failed (maybe already enrolled?): " + e.getMessage());
            }
            psInsert.close();

            // ── Commit everything ─────────────────────────────────
            conn.commit();
            System.out.println("[Transaction] Enrollment committed successfully.");
            return enrollment;

        } catch (SQLException e) {
            // Full rollback on unexpected errors
            conn.rollback();
            System.out.println("[Transaction] Rolled back due to error: " + e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true); // Always restore auto-commit
            conn.close();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ASSIGN GRADE — via Stored Procedure (CallableStatement)
    // ─────────────────────────────────────────────────────────────

    /**
     * CallableStatement Demo:
     *
     * Calls the PostgreSQL stored procedure: assign_grade(student_id, course_id, grade)
     *
     * The procedure is defined in the SQL script and handles the grade
     * update logic on the database side.
     *
     * Syntax: { CALL procedure_name(?, ?, ?) }
     */
    public void assignGrade(int studentId, int courseId, String grade) throws SQLException {
        // { CALL ... } is the JDBC standard syntax for calling stored procedures/functions
        String sql = "{ CALL assign_grade(?, ?, ?) }";

        try (Connection conn = DBConnection.getPooledConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, studentId);    // IN param: student_id
            cs.setInt(2, courseId);     // IN param: course_id
            cs.setString(3, grade);     // IN param: grade

            cs.execute();
            System.out.println("[Stored Procedure] assign_grade executed successfully.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET AVERAGE GPA — via Stored Function (CallableStatement)
    // ─────────────────────────────────────────────────────────────

    /**
     * Calls the PostgreSQL function: get_student_avg(student_id)
     *
     * Functions return a value — we register the OUT/return parameter
     * using registerOutParameter().
     *
     * Syntax: { ? = CALL function_name(?) }
     */
    public double getStudentAvgGpa(int studentId) throws SQLException {
        // ? = CALL means "capture the return value into the first ?"
        String sql = "{ ? = CALL get_student_avg(?) }";

        try (Connection conn = DBConnection.getPooledConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            // Register the return value as a NUMERIC (DOUBLE) OUT parameter
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setInt(2, studentId); // IN param: student_id

            cs.execute();

            double avg = cs.getDouble(1); // Read the return value
            System.out.println("[Stored Function] get_student_avg returned: " + avg);
            return avg;
        }
    }

    // ─── READ: Enrollments for a student ──────────────────────────

    public List<Enrollment> getEnrollmentsByStudent(int studentId) throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        String sql = "SELECT * FROM enrollments WHERE student_id = ?";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ─── READ: All enrollments (with student and course names) ────

    public List<String[]> getAllEnrollmentsWithDetails() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = """
            SELECT e.id, s.name AS student_name, c.name AS course_name,
                   c.credits, COALESCE(e.grade, 'Not Graded') AS grade
            FROM enrollments e
            JOIN students s ON e.student_id = s.id
            JOIN courses  c ON e.course_id  = c.id
            ORDER BY e.id
            """;

        try (Connection conn = DBConnection.getPooledConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("student_name"),
                    rs.getString("course_name"),
                    String.valueOf(rs.getInt("credits")),
                    rs.getString("grade")
                });
            }
        }
        return list;
    }

    // ─── UPDATE grade directly ────────────────────────────────────

    public boolean updateGrade(int enrollmentId, String grade) throws SQLException {
        String sql = "UPDATE enrollments SET grade = ? WHERE id = ?";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, grade);
            ps.setInt(2, enrollmentId);
            return ps.executeUpdate() > 0;
        }
    }

    // ─── DELETE enrollment ────────────────────────────────────────

    public boolean deleteEnrollment(int id) throws SQLException {
        String sql = "DELETE FROM enrollments WHERE id = ?";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ─── Row mapper ───────────────────────────────────────────────

    private Enrollment mapRow(ResultSet rs) throws SQLException {
        return new Enrollment(
            rs.getInt("id"),
            rs.getInt("student_id"),
            rs.getInt("course_id"),
            rs.getString("grade")
        );
    }
}
