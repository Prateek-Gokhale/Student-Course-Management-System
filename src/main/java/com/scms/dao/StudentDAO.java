package com.scms.dao;

import com.scms.model.Student;
import com.scms.util.DBConnection;
import com.scms.util.MetaDataUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * StudentDAO — Data Access Object for Students
 * ============================================================
 *
 * Handles all database operations for the 'students' table.
 *
 * Demonstrates:
 *  - PreparedStatement (parameterized, safe from SQL injection)
 *  - Statement (for simple reads — learning purpose)
 *  - Batch processing (inserting multiple students at once)
 *  - ResultSetMetaData (dynamic column printing)
 */
public class StudentDAO {

    // ─────────────────────────────────────────────────────────────
    // CREATE — Add a single student
    // Uses PreparedStatement to safely bind parameters.
    // ─────────────────────────────────────────────────────────────

    public int addStudent(Student student) throws SQLException {
        // ? placeholders are filled in using setXxx() methods
        String sql = "INSERT INTO students (name, email, dob) VALUES (?, ?, ?) RETURNING id";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, student.getName());
            ps.setString(2, student.getEmail());
            ps.setDate(3, Date.valueOf(student.getDob())); // Convert LocalDate → java.sql.Date

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id"); // Return the generated ID
            }
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────
    // READ — Fetch all students
    // Uses a plain Statement (no parameters needed).
    // Also demonstrates ResultSetMetaData for dynamic column printing.
    // ─────────────────────────────────────────────────────────────

    public List<Student> getAllStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY id";

        try (Connection conn = DBConnection.getPooledConnection();
             // Statement is fine here — no user input, no SQL injection risk
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Optional: print dynamic column info (demonstrates ResultSetMetaData)
            // MetaDataUtil.printResultSetDynamic(rs); // Uncomment to see column metadata

            while (rs.next()) {
                students.add(mapRow(rs));
            }
        }
        return students;
    }

    // ─── Fetch single student by ID ───────────────────────────────

    public Student getStudentById(int id) throws SQLException {
        String sql = "SELECT * FROM students WHERE id = ?";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null; // Student not found
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE — Update student details
    // ─────────────────────────────────────────────────────────────

    public boolean updateStudent(Student student) throws SQLException {
        String sql = "UPDATE students SET name = ?, email = ?, dob = ? WHERE id = ?";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, student.getName());
            ps.setString(2, student.getEmail());
            ps.setDate(3, Date.valueOf(student.getDob()));
            ps.setInt(4, student.getId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0; // true if update was successful
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE — Remove a student by ID
    // ─────────────────────────────────────────────────────────────

    public boolean deleteStudent(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // BATCH INSERT — Insert multiple students in one DB round-trip
    // ─────────────────────────────────────────────────────────────

    /**
     * Batch Processing Demo:
     *
     * Instead of running N separate INSERT statements (N round-trips),
     * we group them into a single batch and send all at once.
     *
     * This is much faster for bulk inserts (100s or 1000s of rows).
     *
     * Returns the number of rows successfully inserted.
     */
    public int batchInsertStudents(List<Student> students) throws SQLException {
        String sql = "INSERT INTO students (name, email, dob) VALUES (?, ?, ?)";

        Connection conn = DBConnection.getPooledConnection();
        try {
            // Disable auto-commit so the whole batch is one transaction
            conn.setAutoCommit(false);

            PreparedStatement ps = conn.prepareStatement(sql);

            for (Student s : students) {
                ps.setString(1, s.getName());
                ps.setString(2, s.getEmail());
                ps.setDate(3, Date.valueOf(s.getDob()));
                ps.addBatch(); // Queue this set of params
            }

            // Execute all queued inserts in one shot
            int[] results = ps.executeBatch();
            conn.commit(); // Commit the transaction

            // Count successfully inserted rows (value = 1 means success)
            int total = 0;
            for (int r : results) total += (r > 0 ? 1 : 0);
            return total;

        } catch (SQLException e) {
            conn.rollback(); // Undo everything if any insert fails
            throw e;
        } finally {
            conn.setAutoCommit(true); // Restore default
            conn.close();
        }
    }

    // ─── Fetch students dynamically (shows ResultSetMetaData) ─────

    /**
     * Demonstrates ResultSetMetaData:
     * Prints column names and types from the result WITHOUT hardcoding them.
     */
    public void printStudentsDynamic() throws SQLException {
        String sql = "SELECT * FROM students ORDER BY id";

        try (Connection conn = DBConnection.getPooledConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n[ResultSetMetaData] Dynamic column printing:");
            MetaDataUtil.printResultSetDynamic(rs);
        }
    }

    // ─── Helper: map ResultSet row → Student object ───────────────

    private Student mapRow(ResultSet rs) throws SQLException {
        return new Student(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getDate("dob").toLocalDate() // java.sql.Date → LocalDate
        );
    }
}
