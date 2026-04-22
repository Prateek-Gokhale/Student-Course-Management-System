package com.scms.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scms.dao.CourseDAO;
import com.scms.dao.EnrollmentDAO;
import com.scms.dao.StudentDAO;
import com.scms.model.Course;
import com.scms.model.Enrollment;
import com.scms.model.Student;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Deployment HTTP server:
 * - Serves React frontend assets
 * - Exposes lightweight JSON API for Students/Courses/Enrollments
 * - Uses database DAOs when DB is available, otherwise falls back to in-memory demo mode
 */
public class DeployServer {

    private static final int DEFAULT_PORT = 10000;
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private static final StudentDAO STUDENT_DAO = new StudentDAO();
    private static final CourseDAO COURSE_DAO = new CourseDAO();
    private static final EnrollmentDAO ENROLLMENT_DAO = new EnrollmentDAO();

    private static final List<Student> FALLBACK_STUDENTS = new CopyOnWriteArrayList<>();
    private static final List<Course> FALLBACK_COURSES = new CopyOnWriteArrayList<>();
    private static final List<Enrollment> FALLBACK_ENROLLMENTS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger STUDENT_SEQ = new AtomicInteger(1000);
    private static final AtomicInteger COURSE_SEQ = new AtomicInteger(2000);
    private static final AtomicInteger ENROLLMENT_SEQ = new AtomicInteger(3000);

    private static volatile DataMode dataMode = DataMode.DATABASE;
    private static volatile String dataModeMessage = "Database mode";

    static {
        seedFallbackData();
    }

    public static void main(String[] args) throws IOException {
        int port = resolvePort();
        Path staticRoot = resolveStaticRoot();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/health", exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handlePreflight(exchange);
                return;
            }
            sendText(exchange, 200, "ok", "text/plain; charset=utf-8");
        });
        server.createContext("/api/health", exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handlePreflight(exchange);
                return;
            }
            sendText(exchange, 200, "ok", "text/plain; charset=utf-8");
        });
        server.createContext("/api/status", exchange -> withErrorHandling(exchange, DeployServer::handleStatus));
        server.createContext("/api/students", exchange -> withErrorHandling(exchange, DeployServer::handleStudents));
        server.createContext("/api/courses", exchange -> withErrorHandling(exchange, DeployServer::handleCourses));
        server.createContext("/api/enrollments", exchange -> withErrorHandling(exchange, DeployServer::handleEnrollments));
        server.createContext("/api/enrollments/grade", exchange -> withErrorHandling(exchange, DeployServer::handleEnrollmentGradeUpdate));
        server.createContext("/", exchange -> handleFrontend(exchange, staticRoot));

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("[DeployServer] Listening on port " + port);
        if (staticRoot != null) {
            System.out.println("[DeployServer] Serving frontend from " + staticRoot);
        } else {
            System.out.println("[DeployServer] Frontend build not found. API-only mode.");
        }
    }

    private static void handleStatus(HttpExchange exchange) throws Exception {
        if (handlePreflight(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("mode", dataMode.name().toLowerCase(Locale.ROOT));
        body.put("message", dataModeMessage);
        body.put("students", loadStudents().size());
        body.put("courses", loadCourses().size());
        body.put("enrollments", loadEnrollmentViews().size());
        sendJson(exchange, 200, body);
    }

    private static void handleStudents(HttpExchange exchange) throws Exception {
        if (handlePreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("GET".equals(method)) {
            sendJson(exchange, 200, Map.of("items", loadStudents()));
            return;
        }
        if ("POST".equals(method)) {
            JsonNode payload = readJsonBody(exchange);
            String name = requiredText(payload, "name");
            String email = requiredText(payload, "email");
            LocalDate dob = LocalDate.parse(requiredText(payload, "dob"));

            Student created = withDbFallback(
                    () -> {
                        int id = STUDENT_DAO.addStudent(new Student(name, email, dob));
                        return new Student(id, name, email, dob);
                    },
                    () -> addStudentFallback(name, email, dob)
            );
            sendJson(exchange, 201, created);
            return;
        }
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private static void handleCourses(HttpExchange exchange) throws Exception {
        if (handlePreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("GET".equals(method)) {
            sendJson(exchange, 200, Map.of("items", loadCourses()));
            return;
        }
        if ("POST".equals(method)) {
            JsonNode payload = readJsonBody(exchange);
            String name = requiredText(payload, "name");
            int credits = requiredInt(payload, "credits");
            if (credits < 1 || credits > 6) {
                throw new IllegalArgumentException("credits must be between 1 and 6");
            }

            Course created = withDbFallback(
                    () -> {
                        int id = COURSE_DAO.addCourse(new Course(name, credits));
                        return new Course(id, name, credits);
                    },
                    () -> addCourseFallback(name, credits)
            );
            sendJson(exchange, 201, created);
            return;
        }
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private static void handleEnrollments(HttpExchange exchange) throws Exception {
        if (handlePreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("GET".equals(method)) {
            sendJson(exchange, 200, Map.of("items", loadEnrollmentViews()));
            return;
        }
        if ("POST".equals(method)) {
            JsonNode payload = readJsonBody(exchange);
            int studentId = requiredInt(payload, "studentId");
            int courseId = requiredInt(payload, "courseId");

            Enrollment enrollment = withDbFallback(
                    () -> ENROLLMENT_DAO.enrollStudentWithTransaction(studentId, courseId),
                    () -> enrollFallback(studentId, courseId)
            );

            sendJson(exchange, 201, toEnrollmentView(enrollment));
            return;
        }
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private static void handleEnrollmentGradeUpdate(HttpExchange exchange) throws Exception {
        if (handlePreflight(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        JsonNode payload = readJsonBody(exchange);
        int enrollmentId = requiredInt(payload, "enrollmentId");
        String grade = requiredText(payload, "grade");

        boolean updated = withDbFallback(
                () -> ENROLLMENT_DAO.updateGrade(enrollmentId, grade),
                () -> updateGradeFallback(enrollmentId, grade)
        );
        if (!updated) {
            sendJson(exchange, 404, Map.of("error", "Enrollment not found"));
            return;
        }
        sendJson(exchange, 200, Map.of("message", "Grade updated"));
    }

    private static void handleFrontend(HttpExchange exchange, Path staticRoot) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        if (staticRoot == null) {
            sendText(
                    exchange,
                    200,
                    "Student Course Management System API is running. Build frontend assets to enable UI.",
                    "text/plain; charset=utf-8"
            );
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        String relativePath = "/".equals(requestPath) ? "index.html" : requestPath.substring(1);
        Path filePath = staticRoot.resolve(relativePath).normalize();

        if (!filePath.startsWith(staticRoot)) {
            sendJson(exchange, 403, Map.of("error", "Forbidden"));
            return;
        }

        if (Files.isDirectory(filePath)) {
            filePath = filePath.resolve("index.html");
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            filePath = staticRoot.resolve("index.html");
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            sendJson(exchange, 404, Map.of("error", "Not found"));
            return;
        }

        byte[] bytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().add("Content-Type", detectContentType(filePath.getFileName().toString()));
        exchange.sendResponseHeaders(200, bytes.length);
        if (!"HEAD".equals(method)) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.getResponseBody().close();
        }
    }

    private static String detectContentType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private static int resolvePort() {
        String value = System.getenv("PORT");
        if (value == null || value.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return DEFAULT_PORT;
        }
    }

    private static Path resolveStaticRoot() {
        String configured = System.getenv("FRONTEND_DIR");
        if (configured != null && !configured.isBlank()) {
            Path configuredPath = Paths.get(configured).toAbsolutePath().normalize();
            if (Files.isDirectory(configuredPath)) {
                return configuredPath;
            }
        }

        Path localPath = Paths.get("frontend", "dist").toAbsolutePath().normalize();
        if (Files.isDirectory(localPath)) {
            return localPath;
        }
        return null;
    }

    private static JsonNode readJsonBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] raw = is.readAllBytes();
            if (raw.length == 0) {
                return OBJECT_MAPPER.createObjectNode();
            }
            return OBJECT_MAPPER.readTree(raw);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid JSON payload");
        }
    }

    private static String requiredText(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || node.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.asText().trim();
    }

    private static int requiredInt(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (!node.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be an integer");
        }
        return node.asInt();
    }

    private static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            return false;
        }
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return true;
    }

    private static void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] payload = OBJECT_MAPPER.writeValueAsBytes(body);
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    private static void sendJsonSafe(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] payload = OBJECT_MAPPER.writeValueAsBytes(body);
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    private static void sendText(HttpExchange exchange, int statusCode, String body, String contentType) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static List<Student> loadStudents() {
        return withDbFallback(STUDENT_DAO::getAllStudents, DeployServer::snapshotFallbackStudents);
    }

    private static List<Course> loadCourses() {
        return withDbFallback(COURSE_DAO::getAllCourses, DeployServer::snapshotFallbackCourses);
    }

    private static List<EnrollmentView> loadEnrollmentViews() {
        return withDbFallback(
                () -> {
                    List<EnrollmentView> result = new ArrayList<>();
                    for (String[] row : ENROLLMENT_DAO.getAllEnrollmentsWithDetails()) {
                        result.add(new EnrollmentView(
                                Integer.parseInt(row[0]),
                                -1,
                                -1,
                                row[1],
                                row[2],
                                Integer.parseInt(row[3]),
                                row[4]
                        ));
                    }
                    return result;
                },
                DeployServer::fallbackEnrollmentViews
        );
    }

    private static Student addStudentFallback(String name, String email, LocalDate dob) {
        Optional<Student> existing = FALLBACK_STUDENTS.stream()
                .filter(s -> s.getEmail().equalsIgnoreCase(email))
                .findAny();
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        Student student = new Student(STUDENT_SEQ.incrementAndGet(), name, email, dob);
        FALLBACK_STUDENTS.add(student);
        return student;
    }

    private static Course addCourseFallback(String name, int credits) {
        Course course = new Course(COURSE_SEQ.incrementAndGet(), name, credits);
        FALLBACK_COURSES.add(course);
        return course;
    }

    private static Enrollment enrollFallback(int studentId, int courseId) {
        Student student = FALLBACK_STUDENTS.stream().filter(s -> s.getId() == studentId).findFirst().orElse(null);
        if (student == null) {
            throw new IllegalArgumentException("Student not found");
        }
        Course course = FALLBACK_COURSES.stream().filter(c -> c.getId() == courseId).findFirst().orElse(null);
        if (course == null) {
            throw new IllegalArgumentException("Course not found");
        }
        boolean exists = FALLBACK_ENROLLMENTS.stream()
                .anyMatch(e -> e.getStudentId() == studentId && e.getCourseId() == courseId);
        if (exists) {
            throw new IllegalArgumentException("Student is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment(ENROLLMENT_SEQ.incrementAndGet(), studentId, courseId, "Not Graded");
        FALLBACK_ENROLLMENTS.add(enrollment);
        return enrollment;
    }

    private static boolean updateGradeFallback(int enrollmentId, String grade) {
        for (Enrollment enrollment : FALLBACK_ENROLLMENTS) {
            if (enrollment.getId() == enrollmentId) {
                enrollment.setGrade(grade);
                return true;
            }
        }
        return false;
    }

    private static EnrollmentView toEnrollmentView(Enrollment enrollment) {
        Student student = withDbFallback(
                () -> STUDENT_DAO.getStudentById(enrollment.getStudentId()),
                () -> FALLBACK_STUDENTS.stream()
                        .filter(s -> s.getId() == enrollment.getStudentId())
                        .findFirst()
                        .orElse(null)
        );
        Course course = withDbFallback(
                () -> COURSE_DAO.getCourseById(enrollment.getCourseId()),
                () -> FALLBACK_COURSES.stream()
                        .filter(c -> c.getId() == enrollment.getCourseId())
                        .findFirst()
                        .orElse(null)
        );

        String studentName = student != null ? student.getName() : "Unknown Student";
        String courseName = course != null ? course.getName() : "Unknown Course";
        int credits = course != null ? course.getCredits() : 0;
        String grade = enrollment.getGrade() == null ? "Not Graded" : enrollment.getGrade();

        return new EnrollmentView(
                enrollment.getId(),
                enrollment.getStudentId(),
                enrollment.getCourseId(),
                studentName,
                courseName,
                credits,
                grade
        );
    }

    private static List<Student> snapshotFallbackStudents() {
        return new ArrayList<>(FALLBACK_STUDENTS);
    }

    private static List<Course> snapshotFallbackCourses() {
        return new ArrayList<>(FALLBACK_COURSES);
    }

    private static List<EnrollmentView> fallbackEnrollmentViews() {
        List<EnrollmentView> views = new ArrayList<>();
        for (Enrollment enrollment : FALLBACK_ENROLLMENTS) {
            views.add(toEnrollmentView(enrollment));
        }
        return views;
    }

    private static void seedFallbackData() {
        if (!FALLBACK_STUDENTS.isEmpty() || !FALLBACK_COURSES.isEmpty()) {
            return;
        }

        Student s1 = new Student(STUDENT_SEQ.incrementAndGet(), "Aarav Sharma", "aarav@example.com", LocalDate.parse("2003-05-12"));
        Student s2 = new Student(STUDENT_SEQ.incrementAndGet(), "Maya Iyer", "maya@example.com", LocalDate.parse("2002-11-20"));
        FALLBACK_STUDENTS.add(s1);
        FALLBACK_STUDENTS.add(s2);

        Course c1 = new Course(COURSE_SEQ.incrementAndGet(), "Database Systems", 4);
        Course c2 = new Course(COURSE_SEQ.incrementAndGet(), "Java Programming", 3);
        FALLBACK_COURSES.add(c1);
        FALLBACK_COURSES.add(c2);

        FALLBACK_ENROLLMENTS.add(new Enrollment(
                ENROLLMENT_SEQ.incrementAndGet(),
                s1.getId(),
                c1.getId(),
                "A"
        ));
        FALLBACK_ENROLLMENTS.add(new Enrollment(
                ENROLLMENT_SEQ.incrementAndGet(),
                s2.getId(),
                c2.getId(),
                "B+"
        ));
    }

    private static String sanitizeError(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return (message == null || message.isBlank()) ? "Request failed" : message;
    }

    private static void markDatabaseUnavailable(Throwable throwable) {
        dataMode = DataMode.FALLBACK;
        dataModeMessage = sanitizeError(throwable);
        System.err.println("[DeployServer] Switching to fallback mode: " + dataModeMessage);
    }

    private static <T> T withDbFallback(CheckedSupplier<T> dbOperation, Supplier<T> fallbackOperation) {
        if (dataMode == DataMode.DATABASE) {
            try {
                return dbOperation.get();
            } catch (ExceptionInInitializerError | NoClassDefFoundError ex) {
                markDatabaseUnavailable(ex);
            } catch (SQLException ex) {
                markDatabaseUnavailable(ex);
            } catch (Exception ex) {
                markDatabaseUnavailable(ex);
            }
        }
        return fallbackOperation.get();
    }

    private static void withErrorHandling(HttpExchange exchange, ApiHandler handler) throws IOException {
        try {
            handler.handle(exchange);
        } catch (IllegalArgumentException ex) {
            sendJsonSafe(exchange, 400, Map.of("error", sanitizeError(ex)));
        } catch (Exception ex) {
            sendJsonSafe(exchange, 500, Map.of("error", sanitizeError(ex)));
        }
    }

    private enum DataMode {
        DATABASE,
        FALLBACK
    }

    private record EnrollmentView(
            int id,
            int studentId,
            int courseId,
            String studentName,
            String courseName,
            int credits,
            String grade
    ) {
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface ApiHandler {
        void handle(HttpExchange exchange) throws Exception;
    }
}
