package com.scms.service;

import com.scms.dao.EnrollmentDAO;
import com.scms.model.Enrollment;

import java.sql.SQLException;
import java.util.List;

/**
 * EnrollmentService — Business Logic Layer for Enrollments
 */
public class EnrollmentService {

    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    // Valid letter grades accepted by the system
    private static final List<String> VALID_GRADES =
        List.of("A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D", "F");

    public Enrollment enrollStudent(int studentId, int courseId) throws SQLException {
        return enrollmentDAO.enrollStudentWithTransaction(studentId, courseId);
    }

    public void assignGrade(int studentId, int courseId, String grade) throws Exception {
        if (!VALID_GRADES.contains(grade.toUpperCase())) {
            throw new IllegalArgumentException("Invalid grade. Must be one of: " + VALID_GRADES);
        }
        enrollmentDAO.assignGrade(studentId, courseId, grade.toUpperCase());
    }

    public double getStudentGpa(int studentId) throws SQLException {
        return enrollmentDAO.getStudentAvgGpa(studentId);
    }

    public boolean updateGrade(int enrollmentId, String grade) throws Exception {
        if (!VALID_GRADES.contains(grade.toUpperCase())) {
            throw new IllegalArgumentException("Invalid grade. Must be one of: " + VALID_GRADES);
        }
        return enrollmentDAO.updateGrade(enrollmentId, grade.toUpperCase());
    }

    public boolean deleteEnrollment(int id) throws SQLException {
        return enrollmentDAO.deleteEnrollment(id);
    }

    public List<Enrollment> getEnrollmentsByStudent(int studentId) throws SQLException {
        return enrollmentDAO.getEnrollmentsByStudent(studentId);
    }

    public List<String[]> getAllEnrollmentsWithDetails() throws SQLException {
        return enrollmentDAO.getAllEnrollmentsWithDetails();
    }
}
