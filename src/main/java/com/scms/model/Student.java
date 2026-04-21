package com.scms.model;

import java.time.LocalDate;

/**
 * Model class representing a Student entity.
 * Maps directly to the 'students' table in the database.
 *
 * Follows the POJO (Plain Old Java Object) pattern —
 * just fields, constructors, getters, setters, and toString.
 */
public class Student {

    private int id;
    private String name;
    private String email;
    private LocalDate dob; // Date of Birth

    // ─── Constructors ──────────────────────────────────────────────

    /** Default constructor (used when creating an empty student object) */
    public Student() {}

    /** Constructor without ID (used when inserting a new student — DB auto-generates ID) */
    public Student(String name, String email, LocalDate dob) {
        this.name = name;
        this.email = email;
        this.dob = dob;
    }

    /** Full constructor (used when fetching a student from the database) */
    public Student(int id, String name, String email, LocalDate dob) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.dob = dob;
    }

    // ─── Getters & Setters ─────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    // ─── toString ──────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("Student{id=%d, name='%s', email='%s', dob=%s}",
                id, name, email, dob);
    }
}
