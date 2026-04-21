package com.scms.model;

/**
 * Model class representing a Course entity.
 * Maps directly to the 'courses' table in the database.
 */
public class Course {

    private int id;
    private String name;
    private int credits;

    // ─── Constructors ──────────────────────────────────────────────

    public Course() {}

    /** Constructor without ID — for inserting new courses */
    public Course(String name, int credits) {
        this.name = name;
        this.credits = credits;
    }

    /** Full constructor — for fetching from DB */
    public Course(int id, String name, int credits) {
        this.id = id;
        this.name = name;
        this.credits = credits;
    }

    // ─── Getters & Setters ─────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    // ─── toString ──────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("Course{id=%d, name='%s', credits=%d}", id, name, credits);
    }
}
