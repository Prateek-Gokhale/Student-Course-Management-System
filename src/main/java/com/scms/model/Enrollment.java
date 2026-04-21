package com.scms.model;

/**
 * Model class representing an Enrollment entity.
 * Maps directly to the 'enrollments' table in the database.
 *
 * An enrollment is the relationship between a Student and a Course,
 * and optionally holds a grade for that student in that course.
 */
public class Enrollment {

    private int id;
    private int studentId;
    private int courseId;
    private String grade; // Can be null if not yet graded (e.g., "A", "B+", "C")

    // ─── Constructors ──────────────────────────────────────────────

    public Enrollment() {}

    /** Constructor without ID and grade — for new enrollments */
    public Enrollment(int studentId, int courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
    }

    /** Full constructor — for fetching from DB */
    public Enrollment(int id, int studentId, int courseId, String grade) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.grade = grade;
    }

    // ─── Getters & Setters ─────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    // ─── toString ──────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("Enrollment{id=%d, studentId=%d, courseId=%d, grade='%s'}",
                id, studentId, courseId, grade);
    }
}
