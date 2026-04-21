package com.scms.dao;

import com.scms.model.Course;
import com.scms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CourseDAO — Data Access Object for Courses
 *
 * Handles all database operations for the 'courses' table.
 * Uses PreparedStatement throughout for safety and performance.
 */
public class CourseDAO {

    // ─── CREATE ───────────────────────────────────────────────────

    public int addCourse(Course course) throws SQLException {
        String sql = "INSERT INTO courses (name, credits) VALUES (?, ?) RETURNING id";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, course.getName());
            ps.setInt(2, course.getCredits());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        }
        return -1;
    }

    // ─── READ ALL ─────────────────────────────────────────────────

    public List<Course> getAllCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses ORDER BY id";

        try (Connection conn = DBConnection.getPooledConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                courses.add(mapRow(rs));
            }
        }
        return courses;
    }

    // ─── READ BY ID ───────────────────────────────────────────────

    public Course getCourseById(int id) throws SQLException {
        String sql = "SELECT * FROM courses WHERE id = ?";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    public boolean updateCourse(Course course) throws SQLException {
        String sql = "UPDATE courses SET name = ?, credits = ? WHERE id = ?";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, course.getName());
            ps.setInt(2, course.getCredits());
            ps.setInt(3, course.getId());

            return ps.executeUpdate() > 0;
        }
    }

    // ─── DELETE ───────────────────────────────────────────────────

    public boolean deleteCourse(int id) throws SQLException {
        String sql = "DELETE FROM courses WHERE id = ?";

        try (Connection conn = DBConnection.getPooledConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ─── Row mapper ───────────────────────────────────────────────

    private Course mapRow(ResultSet rs) throws SQLException {
        return new Course(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getInt("credits")
        );
    }
}
