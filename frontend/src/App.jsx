import { useEffect, useMemo, useState } from "react";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  const raw = await response.text();
  const data = raw ? JSON.parse(raw) : {};

  if (!response.ok) {
    throw new Error(data.error || `Request failed (${response.status})`);
  }
  return data;
}

function StatCard({ label, value }) {
  return (
    <div className="stat-card">
      <p className="stat-label">{label}</p>
      <p className="stat-value">{value}</p>
    </div>
  );
}

function App() {
  const [students, setStudents] = useState([]);
  const [courses, setCourses] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [status, setStatus] = useState({ mode: "loading", message: "Connecting..." });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const [studentForm, setStudentForm] = useState({
    name: "",
    email: "",
    dob: ""
  });
  const [courseForm, setCourseForm] = useState({
    name: "",
    credits: 3
  });
  const [enrollmentForm, setEnrollmentForm] = useState({
    studentId: "",
    courseId: ""
  });
  const [gradeDrafts, setGradeDrafts] = useState({});

  const canEnroll = students.length > 0 && courses.length > 0;

  const stats = useMemo(
    () => [
      { label: "Students", value: students.length },
      { label: "Courses", value: courses.length },
      { label: "Enrollments", value: enrollments.length }
    ],
    [students, courses, enrollments]
  );

  const loadAll = async () => {
    setError("");
    try {
      const [studentData, courseData, enrollmentData, statusData] = await Promise.all([
        request("/api/students"),
        request("/api/courses"),
        request("/api/enrollments"),
        request("/api/status")
      ]);

      setStudents(studentData.items || []);
      setCourses(courseData.items || []);
      setEnrollments(enrollmentData.items || []);
      setStatus(statusData);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const handleAddStudent = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      await request("/api/students", {
        method: "POST",
        body: JSON.stringify(studentForm)
      });
      setStudentForm({ name: "", email: "", dob: "" });
      await loadAll();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleAddCourse = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      await request("/api/courses", {
        method: "POST",
        body: JSON.stringify({
          ...courseForm,
          credits: Number(courseForm.credits)
        })
      });
      setCourseForm({ name: "", credits: 3 });
      await loadAll();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleEnroll = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      await request("/api/enrollments", {
        method: "POST",
        body: JSON.stringify({
          studentId: Number(enrollmentForm.studentId),
          courseId: Number(enrollmentForm.courseId)
        })
      });
      setEnrollmentForm({ studentId: "", courseId: "" });
      await loadAll();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateGrade = async (enrollmentId) => {
    const nextGrade = (gradeDrafts[enrollmentId] || "").trim();
    if (!nextGrade) {
      setError("Please provide a grade before updating.");
      return;
    }

    setSaving(true);
    setError("");
    try {
      await request("/api/enrollments/grade", {
        method: "POST",
        body: JSON.stringify({
          enrollmentId,
          grade: nextGrade
        })
      });
      await loadAll();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page">
      <div className="backdrop backdrop-a" />
      <div className="backdrop backdrop-b" />

      <main className="layout">
        <header className="hero">
          <p className="eyebrow">Student Course Management System</p>
          <h1>React Control Center</h1>
          <p className="subtitle">
            Manage students, courses, enrollments, and grades from one clean dashboard.
          </p>
          <div className="status-pill">
            <span className={`status-dot ${status.mode === "fallback" ? "warn" : "ok"}`} />
            <span>
              Mode: <strong>{status.mode}</strong> {status.message ? `- ${status.message}` : ""}
            </span>
          </div>
        </header>

        <section className="stats-grid">
          {stats.map((card) => (
            <StatCard key={card.label} label={card.label} value={card.value} />
          ))}
        </section>

        {error && <div className="error-banner">{error}</div>}

        <section className="panel-grid">
          <article className="panel">
            <h2>Add Student</h2>
            <form onSubmit={handleAddStudent} className="form-grid">
              <input
                placeholder="Full name"
                value={studentForm.name}
                onChange={(e) => setStudentForm({ ...studentForm, name: e.target.value })}
                required
              />
              <input
                type="email"
                placeholder="Email"
                value={studentForm.email}
                onChange={(e) => setStudentForm({ ...studentForm, email: e.target.value })}
                required
              />
              <input
                type="date"
                value={studentForm.dob}
                onChange={(e) => setStudentForm({ ...studentForm, dob: e.target.value })}
                required
              />
              <button type="submit" disabled={saving}>
                {saving ? "Saving..." : "Create Student"}
              </button>
            </form>
          </article>

          <article className="panel">
            <h2>Add Course</h2>
            <form onSubmit={handleAddCourse} className="form-grid">
              <input
                placeholder="Course name"
                value={courseForm.name}
                onChange={(e) => setCourseForm({ ...courseForm, name: e.target.value })}
                required
              />
              <input
                type="number"
                min="1"
                max="6"
                value={courseForm.credits}
                onChange={(e) => setCourseForm({ ...courseForm, credits: e.target.value })}
                required
              />
              <button type="submit" disabled={saving}>
                {saving ? "Saving..." : "Create Course"}
              </button>
            </form>
          </article>

          <article className="panel">
            <h2>Enroll Student</h2>
            <form onSubmit={handleEnroll} className="form-grid">
              <select
                value={enrollmentForm.studentId}
                onChange={(e) =>
                  setEnrollmentForm({ ...enrollmentForm, studentId: e.target.value })
                }
                required
                disabled={!canEnroll}
              >
                <option value="">Select student</option>
                {students.map((student) => (
                  <option key={student.id} value={student.id}>
                    {student.name}
                  </option>
                ))}
              </select>

              <select
                value={enrollmentForm.courseId}
                onChange={(e) =>
                  setEnrollmentForm({ ...enrollmentForm, courseId: e.target.value })
                }
                required
                disabled={!canEnroll}
              >
                <option value="">Select course</option>
                {courses.map((course) => (
                  <option key={course.id} value={course.id}>
                    {course.name} ({course.credits} credits)
                  </option>
                ))}
              </select>

              <button type="submit" disabled={saving || !canEnroll}>
                {saving ? "Saving..." : "Enroll"}
              </button>
            </form>
          </article>
        </section>

        <section className="table-section">
          <h2>Students</h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>DOB</th>
                </tr>
              </thead>
              <tbody>
                {students.map((student) => (
                  <tr key={student.id}>
                    <td>{student.id}</td>
                    <td>{student.name}</td>
                    <td>{student.email}</td>
                    <td>{student.dob}</td>
                  </tr>
                ))}
                {!students.length && (
                  <tr>
                    <td colSpan="4" className="empty-cell">
                      No students yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="table-section">
          <h2>Enrollments & Grades</h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Student</th>
                  <th>Course</th>
                  <th>Credits</th>
                  <th>Grade</th>
                  <th>Update</th>
                </tr>
              </thead>
              <tbody>
                {enrollments.map((enrollment) => (
                  <tr key={enrollment.id}>
                    <td>{enrollment.id}</td>
                    <td>{enrollment.studentName}</td>
                    <td>{enrollment.courseName}</td>
                    <td>{enrollment.credits}</td>
                    <td>{enrollment.grade}</td>
                    <td>
                      <div className="grade-editor">
                        <input
                          placeholder="A, B+, C..."
                          value={gradeDrafts[enrollment.id] ?? ""}
                          onChange={(e) =>
                            setGradeDrafts((prev) => ({
                              ...prev,
                              [enrollment.id]: e.target.value
                            }))
                          }
                        />
                        <button
                          type="button"
                          onClick={() => handleUpdateGrade(enrollment.id)}
                          disabled={saving}
                        >
                          Save
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!enrollments.length && (
                  <tr>
                    <td colSpan="6" className="empty-cell">
                      No enrollments yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
}

export default App;
