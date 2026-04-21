package com.scms.menu;

import com.scms.model.Course;
import com.scms.model.Enrollment;
import com.scms.model.Student;
import com.scms.service.CourseService;
import com.scms.service.EnrollmentService;
import com.scms.service.PerformanceDemo;
import com.scms.service.StudentService;
import com.scms.util.ConsoleUtil;
import com.scms.util.DBConnection;
import com.scms.util.MetaDataUtil;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminMenu — CLI Menu for the Administrator role.
 *
 * Admin can:
 *  - Manage students (CRUD)
 *  - Manage courses (CRUD)
 *  - View enrollments
 *  - Assign grades
 *  - Run batch insert demo
 *  - View metadata
 *  - Run performance comparison
 */
public class AdminMenu {

    private final StudentService    studentService    = new StudentService();
    private final CourseService     courseService     = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();

    public void show() {
        boolean running = true;
        while (running) {
            ConsoleUtil.printHeader("ADMIN MENU");
            System.out.println("  [1]  Student Management");
            System.out.println("  [2]  Course Management");
            System.out.println("  [3]  View All Enrollments");
            System.out.println("  [4]  Assign Grade");
            System.out.println("  [5]  Batch Insert Students (Demo)");
            System.out.println("  [6]  Database Metadata");
            System.out.println("  [7]  Connection Pool Performance Test");
            System.out.println("  [8]  Dynamic Column Print (ResultSetMetaData)");
            System.out.println("  [0]  Back to Main Menu");
            ConsoleUtil.printDivider();

            String choice = ConsoleUtil.prompt("Enter choice");
            switch (choice) {
                case "1" -> studentManagementMenu();
                case "2" -> courseManagementMenu();
                case "3" -> viewAllEnrollments();
                case "4" -> assignGrade();
                case "5" -> batchInsertDemo();
                case "6" -> showMetadata();
                case "7" -> new PerformanceDemo().runComparison();
                case "8" -> dynamicPrint();
                case "0" -> running = false;
                default  -> ConsoleUtil.printError("Invalid choice.");
            }
        }
    }

    // ─── Student Management Sub-menu ──────────────────────────────

    private void studentManagementMenu() {
        boolean running = true;
        while (running) {
            ConsoleUtil.printHeader("Student Management");
            System.out.println("  [1]  Add Student");
            System.out.println("  [2]  View All Students");
            System.out.println("  [3]  Update Student");
            System.out.println("  [4]  Delete Student");
            System.out.println("  [0]  Back");
            String choice = ConsoleUtil.prompt("Choice");
            switch (choice) {
                case "1" -> addStudent();
                case "2" -> viewAllStudents();
                case "3" -> updateStudent();
                case "4" -> deleteStudent();
                case "0" -> running = false;
                default  -> ConsoleUtil.printError("Invalid choice.");
            }
        }
    }

    private void addStudent() {
        ConsoleUtil.printHeader("Add Student");
        try {
            String name  = ConsoleUtil.prompt("Name");
            String email = ConsoleUtil.prompt("Email");
            String dob   = ConsoleUtil.prompt("Date of Birth (YYYY-MM-DD)");
            Student s = studentService.addStudent(name, email, dob);
            ConsoleUtil.printSuccess("Student added! ID = " + s.getId());
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    private void viewAllStudents() {
        ConsoleUtil.printHeader("All Students");
        try {
            List<Student> students = studentService.getAllStudents();
            List<String[]> rows = new ArrayList<>();
            for (Student s : students) {
                rows.add(new String[]{
                    String.valueOf(s.getId()), s.getName(),
                    s.getEmail(), s.getDob().toString()
                });
            }
            ConsoleUtil.printTable(new String[]{"ID", "Name", "Email", "DOB"}, rows);
            ConsoleUtil.printInfo("Total: " + students.size() + " student(s)");
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    private void updateStudent() {
        ConsoleUtil.printHeader("Update Student");
        try {
            int id      = ConsoleUtil.promptInt("Student ID");
            String name  = ConsoleUtil.prompt("New Name");
            String email = ConsoleUtil.prompt("New Email");
            String dob   = ConsoleUtil.prompt("New DOB (YYYY-MM-DD)");
            boolean ok = studentService.updateStudent(id, name, email, dob);
            if (ok) ConsoleUtil.printSuccess("Student updated.");
            else    ConsoleUtil.printError("Student not found.");
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    private void deleteStudent() {
        ConsoleUtil.printHeader("Delete Student");
        try {
            int id = ConsoleUtil.promptInt("Student ID to delete");
            boolean ok = studentService.deleteStudent(id);
            if (ok) ConsoleUtil.printSuccess("Student deleted.");
            else    ConsoleUtil.printError("Student not found.");
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    // ─── Course Management Sub-menu ───────────────────────────────

    private void courseManagementMenu() {
        boolean running = true;
        while (running) {
            ConsoleUtil.printHeader("Course Management");
            System.out.println("  [1]  Add Course");
            System.out.println("  [2]  View All Courses");
            System.out.println("  [3]  Update Course");
            System.out.println("  [4]  Delete Course");
            System.out.println("  [0]  Back");
            String choice = ConsoleUtil.prompt("Choice");
            switch (choice) {
                case "1" -> addCourse();
                case "2" -> viewAllCourses();
                case "3" -> updateCourse();
                case "4" -> deleteCourse();
                case "0" -> running = false;
                default  -> ConsoleUtil.printError("Invalid choice.");
            }
        }
    }

    private void addCourse() {
        ConsoleUtil.printHeader("Add Course");
        try {
            String name = ConsoleUtil.prompt("Course Name");
            int credits = ConsoleUtil.promptInt("Credits (1-6)");
            Course c = courseService.addCourse(name, credits);
            ConsoleUtil.printSuccess("Course added! ID = " + c.getId());
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    private void viewAllCourses() {
        ConsoleUtil.printHeader("All Courses");
        try {
            List<Course> courses = courseService.getAllCourses();
            List<String[]> rows = new ArrayList<>();
            for (Course c : courses) {
                rows.add(new String[]{
                    String.valueOf(c.getId()), c.getName(), String.valueOf(c.getCredits())
                });
            }
            ConsoleUtil.printTable(new String[]{"ID", "Course Name", "Credits"}, rows);
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    private void updateCourse() {
        ConsoleUtil.printHeader("Update Course");
        try {
            int id      = ConsoleUtil.promptInt("Course ID");
            String name = ConsoleUtil.prompt("New Name");
            int credits = ConsoleUtil.promptInt("New Credits");
            boolean ok = courseService.updateCourse(id, name, credits);
            if (ok) ConsoleUtil.printSuccess("Course updated.");
            else    ConsoleUtil.printError("Course not found.");
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    private void deleteCourse() {
        ConsoleUtil.printHeader("Delete Course");
        try {
            int id = ConsoleUtil.promptInt("Course ID to delete");
            boolean ok = courseService.deleteCourse(id);
            if (ok) ConsoleUtil.printSuccess("Course deleted.");
            else    ConsoleUtil.printError("Course not found.");
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    // ─── Other Admin Functions ────────────────────────────────────

    private void viewAllEnrollments() {
        ConsoleUtil.printHeader("All Enrollments");
        try {
            List<String[]> rows = enrollmentService.getAllEnrollmentsWithDetails();
            ConsoleUtil.printTable(
                new String[]{"Enroll ID", "Student", "Course", "Credits", "Grade"},
                rows
            );
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    private void assignGrade() {
        ConsoleUtil.printHeader("Assign Grade (via Stored Procedure)");
        try {
            int studentId = ConsoleUtil.promptInt("Student ID");
            int courseId  = ConsoleUtil.promptInt("Course ID");
            String grade  = ConsoleUtil.prompt("Grade (e.g., A, B+, C-)");
            enrollmentService.assignGrade(studentId, courseId, grade);
            ConsoleUtil.printSuccess("Grade assigned via stored procedure!");
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    private void batchInsertDemo() {
        ConsoleUtil.printHeader("Batch Insert Demo");
        ConsoleUtil.printInfo("Inserting 5 sample students using JDBC Batch Processing...");
        try {
            List<Student> students = List.of(
                new Student("Alice Batch",   "alice@demo.com",   LocalDate.of(2000, 1, 15)),
                new Student("Bob Batch",     "bob@demo.com",     LocalDate.of(1999, 5, 20)),
                new Student("Carol Batch",   "carol@demo.com",   LocalDate.of(2001, 8, 10)),
                new Student("David Batch",   "david@demo.com",   LocalDate.of(2000, 3, 25)),
                new Student("Eve Batch",     "eve@demo.com",     LocalDate.of(1998, 12, 5))
            );
            int inserted = studentService.batchInsertStudents(students);
            ConsoleUtil.printSuccess("Batch insert complete! " + inserted + " students inserted.");
        } catch (Exception e) {
            ConsoleUtil.printError("Batch insert failed: " + e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    private void showMetadata() {
        ConsoleUtil.printHeader("Database Metadata");
        try (Connection conn = DBConnection.getPooledConnection()) {
            MetaDataUtil.printDatabaseInfo(conn);
            MetaDataUtil.listTables(conn);

            System.out.println();
            String tableName = ConsoleUtil.prompt("Enter table name to inspect columns (or ENTER to skip)");
            if (!tableName.isBlank()) {
                MetaDataUtil.printColumnInfo(conn, tableName);
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    private void dynamicPrint() {
        ConsoleUtil.printHeader("Dynamic ResultSet Print (ResultSetMetaData)");
        try {
            studentService.printStudentsDynamic();
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }
}
