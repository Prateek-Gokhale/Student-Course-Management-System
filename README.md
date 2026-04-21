# 📚 Student Course Management System (SCMS)

A **production-ready, beginner-friendly Java CLI application** that demonstrates every major JDBC concept in a real-world context — built with Java 17, PostgreSQL, and HikariCP.

> **Perfect for**: Java learners, college projects, portfolio pieces, and anyone preparing for backend developer interviews.

---

## 🗂️ Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [JDBC Concepts Covered](#-jdbc-concepts-covered)
- [Database Design](#-database-design)
- [Prerequisites](#-prerequisites)
- [Setup & Installation](#-setup--installation)
- [Running the Application](#-running-the-application)
- [Sample Output](#-sample-output)
- [Architecture Explained](#-architecture-explained)
- [SQL Scripts Reference](#-sql-scripts-reference)
- [Learning Highlights](#-learning-highlights)

---

## ✨ Features

### Admin Role
| Feature | Details |
|--------|---------|
| Student CRUD | Add, view, update, delete students |
| Course CRUD | Add, view, update, delete courses |
| View Enrollments | See all enrollments with student/course names and grades |
| Assign Grades | Via PostgreSQL stored procedure (`assign_grade`) |
| Batch Insert | Insert 5 demo students using JDBC batch execution |
| Database Metadata | View DB info, table list, column details |
| Performance Test | Compare pooled vs non-pooled connection speed |
| Dynamic Print | Print any ResultSet without hardcoded column names |

### Student Role
| Feature | Details |
|--------|---------|
| Register | Create a new student account |
| Login | Simple session by student ID |
| Update Profile | Change name, email, DOB |
| Browse Courses | View all available courses |
| Enroll | Enroll in a course (with full transaction + savepoint) |
| View Grades | See all enrolled courses and grades |
| View GPA | Calculated via PostgreSQL stored function (`get_student_avg`) |

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java (JDK) | 17+ | Language |
| PostgreSQL | 14+ | Database |
| JDBC | 4.2 | Database connectivity |
| HikariCP | 5.1.0 | Connection pooling |
| Maven | 3.8+ | Build tool |
| SLF4J Simple | 2.0.12 | Logging (for HikariCP) |

---

## 📁 Project Structure

```
student-course-management/
│
├── pom.xml                          # Maven dependencies & build config
│
├── sql/
│   ├── 01_schema.sql                # Create tables + indexes
│   ├── 02_procedures.sql            # Stored procedure + function
│   ├── 03_seed_data.sql             # Sample data (8 students, 8 courses)
│   └── 04_reset.sql                 # Drop everything (dev use)
│
└── src/main/
    ├── resources/
    │   └── db.properties            # DB URL, credentials, pool config
    │
    └── java/com/scms/
        ├── model/
        │   ├── Student.java         # POJO for students table
        │   ├── Course.java          # POJO for courses table
        │   └── Enrollment.java      # POJO for enrollments table
        │
        ├── dao/
        │   ├── StudentDAO.java      # SQL for students (CRUD + batch)
        │   ├── CourseDAO.java       # SQL for courses (CRUD)
        │   └── EnrollmentDAO.java   # SQL + stored proc/func + transactions
        │
        ├── service/
        │   ├── StudentService.java  # Business logic for students
        │   ├── CourseService.java   # Business logic for courses
        │   ├── EnrollmentService.java # Business logic for enrollments
        │   └── PerformanceDemo.java # Pool vs non-pool benchmark
        │
        ├── util/
        │   ├── DBConnection.java    # 3 connection methods + HikariCP pool
        │   ├── ConsoleUtil.java     # Pretty table printer, prompts, colors
        │   └── MetaDataUtil.java    # DatabaseMetaData + ResultSetMetaData
        │
        ├── menu/
        │   ├── AdminMenu.java       # Admin CLI menu system
        │   └── StudentMenu.java     # Student CLI menu system
        │
        └── main/
            └── Main.java            # Application entry point
```

---

## 🔌 JDBC Concepts Covered

### 1. Three Connection Methods (`DBConnection.java`)

```java
// Method 1: URL + username + password (most common)
DriverManager.getConnection(url, username, password);

// Method 2: Single URL with embedded credentials
DriverManager.getConnection("jdbc:postgresql://localhost/studentdb?user=postgres&password=...");

// Method 3: Properties object (allows extra driver config)
Properties props = new Properties();
props.setProperty("user", "postgres");
props.setProperty("ApplicationName", "SCMS");
DriverManager.getConnection(url, props);
```

### 2. Statement Types

| Type | Used For | Location |
|------|---------|---------|
| `Statement` | Simple SELECT with no params | `getAllStudents()`, `getAllCourses()` |
| `PreparedStatement` | Parameterized queries (safe) | All INSERT/UPDATE/DELETE |
| `CallableStatement` | Stored procedures/functions | `assignGrade()`, `getStudentAvgGpa()` |

### 3. Batch Processing (`StudentDAO.java`)
```java
PreparedStatement ps = conn.prepareStatement(sql);
for (Student s : students) {
    ps.setString(1, s.getName());
    ps.addBatch(); // Queue the statement
}
int[] results = ps.executeBatch(); // Execute all at once
conn.commit();
```

### 4. Transactions & Savepoints (`EnrollmentDAO.java`)
```java
conn.setAutoCommit(false);
// ... validation queries ...
Savepoint sp = conn.setSavepoint("AFTER_VALIDATION");
try {
    // ... insert enrollment ...
    conn.commit();
} catch (SQLException e) {
    conn.rollback(sp); // Roll back only the insert
}
```

### 5. Stored Procedure (CallableStatement)
```java
// Calling assign_grade(student_id, course_id, grade)
CallableStatement cs = conn.prepareCall("{ CALL assign_grade(?, ?, ?) }");
cs.setInt(1, studentId);
cs.setInt(2, courseId);
cs.setString(3, "A+");
cs.execute();
```

### 6. Stored Function (CallableStatement with return value)
```java
// Calling get_student_avg(student_id) → returns NUMERIC
CallableStatement cs = conn.prepareCall("{ ? = CALL get_student_avg(?) }");
cs.registerOutParameter(1, Types.NUMERIC); // Capture return value
cs.setInt(2, studentId);
cs.execute();
double gpa = cs.getDouble(1);
```

### 7. HikariCP Connection Pool
```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl(DB_URL);
config.setMaximumPoolSize(10);
config.setMinimumIdle(2);
HikariDataSource ds = new HikariDataSource(config);

// Borrow from pool (fast — no TCP handshake)
Connection conn = ds.getConnection();
// ... use connection ...
conn.close(); // Returns to pool, NOT destroyed
```

### 8. DatabaseMetaData
```java
DatabaseMetaData meta = connection.getMetaData();
meta.getDatabaseProductName();   // "PostgreSQL"
meta.getDriverVersion();         // "42.7.3"
meta.getTables(null, "public", "%", new String[]{"TABLE"});
meta.getColumns(null, "public", "students", "%");
```

### 9. ResultSetMetaData (Dynamic column printing)
```java
ResultSetMetaData rsMeta = rs.getMetaData();
int colCount = rsMeta.getColumnCount();
for (int i = 1; i <= colCount; i++) {
    String name = rsMeta.getColumnName(i);
    String type = rsMeta.getColumnTypeName(i);
}
```

---

## 🗄️ Database Design

```
┌──────────────────┐         ┌──────────────────────┐         ┌────────────────────┐
│    students      │         │     enrollments       │         │      courses       │
├──────────────────┤         ├──────────────────────┤         ├────────────────────┤
│ id   (SERIAL) PK │◄───────►│ id         (SERIAL)PK│◄───────►│ id   (SERIAL)   PK │
│ name (VARCHAR)   │    1:N  │ student_id (INT)   FK │  N:1   │ name (VARCHAR)     │
│ email(VARCHAR)   │         │ course_id  (INT)   FK │         │ credits (INT)      │
│ dob  (DATE)      │         │ grade      (VARCHAR)  │         └────────────────────┘
└──────────────────┘         └──────────────────────┘
```

**Constraints:**
- `students.email` → UNIQUE (no duplicate accounts)
- `enrollments(student_id, course_id)` → UNIQUE (no double enrollment)
- `enrollments.student_id` → ON DELETE CASCADE (remove student = remove their enrollments)
- `courses.credits` → CHECK (1–6)

---

## ✅ Prerequisites

1. **Java 17+** — [Download JDK](https://adoptium.net/)
2. **PostgreSQL 14+** — [Download PostgreSQL](https://www.postgresql.org/download/)
3. **Maven 3.8+** — [Download Maven](https://maven.apache.org/download.cgi)
4. A terminal / command prompt

> Check your versions:
> ```bash
> java -version
> psql --version
> mvn -version
> ```

---

## 🚀 Setup & Installation

### Step 1: Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/student-course-management.git
cd student-course-management
```

### Step 2: Create the database and run SQL scripts

Open a terminal and connect to PostgreSQL:

```bash
psql -U postgres
```

Then run the scripts in order:

```sql
-- Create the database
CREATE DATABASE studentdb;
\q
```

```bash
# Create tables
psql -U postgres -d studentdb -f sql/01_schema.sql

# Create stored procedure and function
psql -U postgres -d studentdb -f sql/02_procedures.sql

# (Optional) Load sample data
psql -U postgres -d studentdb -f sql/03_seed_data.sql
```

### Step 3: Configure database credentials

Edit `src/main/resources/db.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/studentdb
db.username=postgres
db.password=YOUR_PASSWORD_HERE
```

### Step 4: Build the project

```bash
mvn clean package
```

This creates a fat JAR at `target/scms.jar` with all dependencies bundled.

---

## ▶️ Running the Application

```bash
java -jar target/scms.jar
```

You'll see the SCMS banner and the role selection menu.

**Quick Demo Flow:**
1. Choose **[1] Admin**
2. Go to **Student Management → View All Students** (see seed data)
3. Go to **Course Management → View All Courses**
4. Try **Assign Grade** for student 4, course 1
5. Run **Connection Pool Performance Test**
6. Go back → choose **[2] Student**
7. **Login** with ID `1` (Arjun Sharma)
8. **View My Grades** and **View My GPA**

---

## 🖥️ Sample Output

```
  ███████╗ ██████╗███╗   ███╗███████╗
  ██╔════╝██╔════╝████╗ ████║██╔════╝
  ███████╗██║     ██╔████╔██║███████╗
  ╚════██║██║     ██║╚██╔╝██║╚════██║
  ███████║╚██████╗██║ ╚═╝ ██║███████║
  ╚══════╝ ╚═════╝╚═╝     ╚═╝╚══════╝

  Student Course Management System v1.0
  Built with Java 17 + JDBC + PostgreSQL + HikariCP

[Startup] Testing database connection...
[Pool] HikariCP pool initialized: SCMS-HikariPool
✔  Database connection successful!

╔══════════════════════════════════════════════╗
║  SCMS — Main Menu                            ║
╚══════════════════════════════════════════════╝
  [1]  Admin   (manage students, courses, grades)
  [2]  Student (register, enroll, view grades)
  [0]  Exit
```

```
┌────┬─────────────────┬────────────────────────────┬────────────┐
│ ID │ Name            │ Email                      │ DOB        │
├────┼─────────────────┼────────────────────────────┼────────────┤
│ 1  │ Arjun Sharma    │ arjun.sharma@example.com   │ 2001-03-15 │
│ 2  │ Priya Nair      │ priya.nair@example.com     │ 2000-07-22 │
│ 3  │ Rahul Gupta     │ rahul.gupta@example.com    │ 2002-01-08 │
└────┴─────────────────┴────────────────────────────┴────────────┘
```

```
╔══════════════════════════════════════════════════╗
║       Connection Pool Performance Comparison     ║
╚══════════════════════════════════════════════════╝
  Running 50 queries each way...

  [Without Pool] 50 queries: 1423 ms
  [With Pool]    50 queries:   87 ms

  Without pooling (DriverManager): 1423 ms
  With HikariCP pooling:             87 ms
  Pool speedup:                    16.4x faster
```

---

## 🏗️ Architecture Explained

The project follows a **Layered / Clean Architecture** pattern:

```
Menu (UI Layer)
    │  Calls service methods, handles user input/output
    ▼
Service Layer
    │  Validates input, applies business rules
    │  Never contains SQL
    ▼
DAO Layer
    │  Only contains SQL operations
    │  Returns model objects or primitives
    ▼
Database (PostgreSQL)
```

**Why this separation matters:**
- **Model** — Pure data, no logic
- **DAO** — Database access only; swap DB without touching services
- **Service** — Business rules only; no SQL knowledge
- **Util** — Reusable helpers (connection, printing, metadata)
- **Menu** — UI only; swap CLI for REST API without changing service/dao

---

## 📜 SQL Scripts Reference

| File | Purpose | Run When |
|------|---------|---------|
| `01_schema.sql` | Creates tables + indexes | First time setup |
| `02_procedures.sql` | Stored procedure + function | After schema |
| `03_seed_data.sql` | 8 students, 8 courses, 21 enrollments | Optional demo data |
| `04_reset.sql` | Drops all tables and objects | Dev reset only |

---

## 🎓 Learning Highlights

| Concept | File | Line Range |
|---------|------|-----------|
| Connection Method 1 (URL+User+Pass) | `DBConnection.java` | `getConnectionMethod1()` |
| Connection Method 2 (Embedded URL) | `DBConnection.java` | `getConnectionMethod2()` |
| Connection Method 3 (Properties) | `DBConnection.java` | `getConnectionMethod3()` |
| HikariCP Pool Setup | `DBConnection.java` | `initHikariPool()` |
| Statement (no params) | `StudentDAO.java` | `getAllStudents()` |
| PreparedStatement | `StudentDAO.java` | `addStudent()` |
| Batch Processing | `StudentDAO.java` | `batchInsertStudents()` |
| Transaction + Savepoint | `EnrollmentDAO.java` | `enrollStudentWithTransaction()` |
| CallableStatement (procedure) | `EnrollmentDAO.java` | `assignGrade()` |
| CallableStatement (function) | `EnrollmentDAO.java` | `getStudentAvgGpa()` |
| DatabaseMetaData | `MetaDataUtil.java` | `printDatabaseInfo()` |
| ResultSetMetaData | `MetaDataUtil.java` | `printResultSetDynamic()` |
| Pool Performance Comparison | `PerformanceDemo.java` | `runComparison()` |

---

## 🔧 Troubleshooting

**"Cannot connect to database"**
- Make sure PostgreSQL is running: `sudo service postgresql start`
- Verify credentials in `db.properties`
- Check the database exists: `psql -U postgres -l`

**"Stored procedure not found"**
- Run `sql/02_procedures.sql` on the `studentdb` database

**"Class not found: org.postgresql.Driver"**
- Run `mvn clean package` to rebuild the fat JAR

**Password authentication failed**
- Update `db.password` in `src/main/resources/db.properties`

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first.

---

*Built with ❤️ for Java learners. Star ⭐ the repo if it helped you!*
