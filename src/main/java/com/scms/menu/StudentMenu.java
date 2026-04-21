package com.scms.menu;

import com.scms.model.Course;
import com.scms.model.Enrollment;
import com.scms.model.Student;
import com.scms.service.CourseService;
import com.scms.service.EnrollmentService;
import com.scms.service.StudentService;
import com.scms.util.ConsoleUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * StudentMenu — CLI Menu for the Student role.
 *
 * Students can:
 *  - Register (create account)
 *  - Update their profile
 *  - Browse and enroll in courses
 *  - View their grades and GPA
 */
public class StudentMenu {

    private final StudentService    studentService    = new StudentService();
    private final CourseService     courseService     = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();

    // The currently "logged in" student (simple session simulation)
    private Student currentStudent = null;

    public void show() {
        boolean running = true;
        while (running) {
            ConsoleUtil.printHeader("STUDENT PORTAL");

            if (currentStudent == null) {
                System.out.println("  [1]  Register");
                System.out.println("  [2]  Login (Enter Student ID)");
                System.out.println("  [0]  Back to Main Menu");
            } else {
                System.out.println("  Logged in as: " + ConsoleUtil.GREEN
                        + currentStudent.getName() + ConsoleUtil.RESET
                        + " (ID: " + currentStudent.getId() + ")");
                ConsoleUtil.printDivider();
                System.out.println("  [1]  Update My Profile");
                System.out.println("  [2]  Browse Courses");
                System.out.println("  [3]  Enroll in a Course");
                System.out.println("  [4]  View My Enrollments & Grades");
                System.out.println("  [5]  View My GPA");
                System.out.println("  [6]  Logout");
                System.out.println("  [0]  Back to Main Menu");
            }

            ConsoleUtil.printDivider();
            String choice = ConsoleUtil.prompt("Choice");

            if (currentStudent == null) {
                switch (choice) {
                    case "1" -> register();
                    case "2" -> login();
                    case "0" -> running = false;
                    default  -> ConsoleUtil.printError("Invalid choice.");
                }
            } else {
                switch (choice) {
                    case "1" -> updateProfile();
                    case "2" -> browseCourses();
                    case "3" -> enrollInCourse();
                    case "4" -> viewGrades();
                    case "5" -> viewGpa();
                    case "6" -> { currentStudent = null; ConsoleUtil.printSuccess("Logged out."); }
                    case "0" -> running = false;
                    default  -> ConsoleUtil.printError("Invalid choice.");
                }
            }
        }
    }

    // ─── Register new student ─────────────────────────────────────

    private void register() {
        ConsoleUtil.printHeader("Student Registration");
        try {
            String name  = ConsoleUtil.prompt("Full Name");
            String email = ConsoleUtil.prompt("Email");
            String dob   = ConsoleUtil.prompt("Date of Birth (YYYY-MM-DD)");

            Student s = studentService.addStudent(name, email, dob);
            currentStudent = s;
            ConsoleUtil.printSuccess("Registration successful! Your Student ID is: " + s.getId());
            ConsoleUtil.printInfo("Please save your ID — you'll need it to log in.");
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    // ─── Login ────────────────────────────────────────────────────

    private void login() {
        ConsoleUtil.printHeader("Student Login");
        try {
            int id = ConsoleUtil.promptInt("Enter your Student ID");
            Student s = studentService.getStudentById(id);
            if (s != null) {
                currentStudent = s;
                ConsoleUtil.printSuccess("Welcome back, " + s.getName() + "!");
            } else {
                ConsoleUtil.printError("No student found with ID " + id);
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    // ─── Update profile ───────────────────────────────────────────

    private void updateProfile() {
        ConsoleUtil.printHeader("Update Profile");
        ConsoleUtil.printInfo("Current: " + currentStudent.getName() + " | " + currentStudent.getEmail());
        try {
            String name  = ConsoleUtil.prompt("New Name (ENTER to keep current)");
            String email = ConsoleUtil.prompt("New Email (ENTER to keep current)");
            String dob   = ConsoleUtil.prompt("New DOB (ENTER to keep current, format YYYY-MM-DD)");

            if (name.isBlank())  name  = currentStudent.getName();
            if (email.isBlank()) email = currentStudent.getEmail();
            if (dob.isBlank())   dob   = currentStudent.getDob().toString();

            boolean ok = studentService.updateStudent(currentStudent.getId(), name, email, dob);
            if (ok) {
                currentStudent = studentService.getStudentById(currentStudent.getId());
                ConsoleUtil.printSuccess("Profile updated successfully!");
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    // ─── Browse courses ───────────────────────────────────────────

    private void browseCourses() {
        ConsoleUtil.printHeader("Available Courses");
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

    // ─── Enroll in a course ───────────────────────────────────────

    private void enrollInCourse() {
        ConsoleUtil.printHeader("Enroll in Course");
        try {
            // Show available courses first
            browseCourses();

            int courseId = ConsoleUtil.promptInt("Enter Course ID to enroll");
            Enrollment enrollment = enrollmentService.enrollStudent(currentStudent.getId(), courseId);

            if (enrollment != null) {
                ConsoleUtil.printSuccess("Successfully enrolled! Enrollment ID: " + enrollment.getId());
            }
        } catch (Exception e) {
            ConsoleUtil.printError("Enrollment failed: " + e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    // ─── View grades ──────────────────────────────────────────────

    private void viewGrades() {
        ConsoleUtil.printHeader("My Grades");
        try {
            List<Enrollment> enrollments = enrollmentService.getEnrollmentsByStudent(currentStudent.getId());

            if (enrollments.isEmpty()) {
                ConsoleUtil.printInfo("You are not enrolled in any courses.");
            } else {
                List<String[]> rows = new ArrayList<>();
                for (Enrollment e : enrollments) {
                    // Fetch course name
                    Course c = courseService.getCourseById(e.getCourseId());
                    String courseName = (c != null) ? c.getName() : "Unknown";
                    String grade = (e.getGrade() != null) ? e.getGrade() : "Not Graded";
                    rows.add(new String[]{
                        String.valueOf(e.getId()), courseName, grade
                    });
                }
                ConsoleUtil.printTable(new String[]{"Enroll ID", "Course", "Grade"}, rows);
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }

    // ─── View GPA ─────────────────────────────────────────────────

    private void viewGpa() {
        ConsoleUtil.printHeader("My GPA (via Stored Function)");
        try {
            double gpa = enrollmentService.getStudentGpa(currentStudent.getId());
            if (gpa == 0.0) {
                ConsoleUtil.printInfo("No graded courses yet. GPA = 0.00");
            } else {
                System.out.println();
                System.out.println("  " + ConsoleUtil.BOLD + ConsoleUtil.GREEN
                        + "Your GPA: " + String.format("%.2f", gpa) + " / 4.00"
                        + ConsoleUtil.RESET);
                System.out.println();
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.waitForEnter();
    }
}
