package com.scms.service;

import com.scms.dao.StudentDAO;
import com.scms.model.Student;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * StudentService — Business Logic Layer for Students
 *
 * Sits between the Menu (UI) and the DAO (database).
 * Handles input validation and delegates to StudentDAO.
 *
 * Rule: Never put SQL in Service. Never put business rules in DAO.
 */
public class StudentService {

    private final StudentDAO studentDAO = new StudentDAO();

    // ─── Add student ──────────────────────────────────────────────

    public Student addStudent(String name, String email, String dobStr) throws Exception {
        // Validate inputs
        if (name == null || name.isBlank())  throw new IllegalArgumentException("Name cannot be empty.");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("Invalid email address.");

        LocalDate dob;
        try {
            dob = LocalDate.parse(dobStr); // Expects "YYYY-MM-DD"
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD.");
        }

        Student student = new Student(name.trim(), email.trim(), dob);
        int newId = studentDAO.addStudent(student);
        student.setId(newId);
        return student;
    }

    // ─── Update student ───────────────────────────────────────────

    public boolean updateStudent(int id, String name, String email, String dobStr) throws Exception {
        if (name == null || name.isBlank())  throw new IllegalArgumentException("Name cannot be empty.");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("Invalid email.");

        LocalDate dob = LocalDate.parse(dobStr);
        Student student = new Student(id, name.trim(), email.trim(), dob);
        return studentDAO.updateStudent(student);
    }

    // ─── Delete student ───────────────────────────────────────────

    public boolean deleteStudent(int id) throws SQLException {
        if (id <= 0) throw new IllegalArgumentException("Invalid student ID.");
        return studentDAO.deleteStudent(id);
    }

    // ─── Get all students ─────────────────────────────────────────

    public List<Student> getAllStudents() throws SQLException {
        return studentDAO.getAllStudents();
    }

    // ─── Get student by ID ────────────────────────────────────────

    public Student getStudentById(int id) throws SQLException {
        return studentDAO.getStudentById(id);
    }

    // ─── Batch insert ─────────────────────────────────────────────

    public int batchInsertStudents(List<Student> students) throws SQLException {
        if (students == null || students.isEmpty()) {
            throw new IllegalArgumentException("Student list is empty.");
        }
        return studentDAO.batchInsertStudents(students);
    }

    // ─── Dynamic print ───────────────────────────────────────────

    public void printStudentsDynamic() throws SQLException {
        studentDAO.printStudentsDynamic();
    }
}
