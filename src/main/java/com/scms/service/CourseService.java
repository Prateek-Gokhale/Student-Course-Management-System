package com.scms.service;

import com.scms.dao.CourseDAO;
import com.scms.model.Course;

import java.sql.SQLException;
import java.util.List;

/**
 * CourseService — Business Logic Layer for Courses
 */
public class CourseService {

    private final CourseDAO courseDAO = new CourseDAO();

    public Course addCourse(String name, int credits) throws Exception {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Course name cannot be empty.");
        if (credits < 1 || credits > 6)    throw new IllegalArgumentException("Credits must be between 1 and 6.");

        Course course = new Course(name.trim(), credits);
        int newId = courseDAO.addCourse(course);
        course.setId(newId);
        return course;
    }

    public boolean updateCourse(int id, String name, int credits) throws Exception {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Course name cannot be empty.");
        return courseDAO.updateCourse(new Course(id, name.trim(), credits));
    }

    public boolean deleteCourse(int id) throws SQLException {
        return courseDAO.deleteCourse(id);
    }

    public List<Course> getAllCourses() throws SQLException {
        return courseDAO.getAllCourses();
    }

    public Course getCourseById(int id) throws SQLException {
        return courseDAO.getCourseById(id);
    }
}
