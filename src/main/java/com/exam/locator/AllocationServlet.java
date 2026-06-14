package com.exam.locator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@WebServlet("/allocate")
public class AllocationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if ("sendEmails".equals(action)) {
            if (!isAdminLoggedIn(request)) {
                response.sendRedirect(request.getContextPath() + "/login.jsp");
                return;
            }

            try {
                List<Student> activeAllocations = fetchAllAllocationsFromDB();
                EmailUtil.sendAllocationEmails(activeAllocations, loadMailConfig());
                request.setAttribute("emailStatus", "Email broadcast started. Keep this page open to watch progress.");
            } catch (Exception e) {
                request.setAttribute("emailStatus", "Broadcast Error: " + e.getMessage());
            }

            request.getRequestDispatcher("admin.jsp").forward(request, response);
            return;
        }

        if ("logout".equals(action)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        loadSearchOptions(request);
        findStudentAllocation(request);
        request.getRequestDispatcher("result.jsp").forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadSearchOptions(request);
        request.getRequestDispatcher("result.jsp").forward(request, response);
    }

    private void findStudentAllocation(HttpServletRequest request) {
        String searchUsn = request.getParameter("usn");
        if (searchUsn == null || searchUsn.trim().isEmpty()) {
            return;
        }

        String cleanUsn = searchUsn.trim().toUpperCase();
        int examId = selectedExamId(request);
        request.setAttribute("selectedExamId", examId);

        String sqlQuery = "SELECT s.Name, s.Email, a.RoomID, a.BenchNumber, a.SeatPosition, "
                + "e.Subject, e.ExamDate, e.ExamTime "
                + "FROM Students s "
                + "LEFT JOIN Seat_Allocations a ON s.USN = a.USN AND a.ExamID = ? "
                + "LEFT JOIN Exams e ON e.ExamID = ? "
                + "WHERE s.USN = ?";

        try {
            DBUtil.ensureSchema();
        } catch (SQLException e) {
            log("Database exception while preparing schema", e);
            request.setAttribute("searchError", "Unable to prepare database now. Please try again later.");
            return;
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlQuery)) {

            ps.setInt(1, examId);
            ps.setInt(2, examId);
            ps.setString(3, cleanUsn);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student matchedStudent = toStudent(cleanUsn, rs);
                    request.setAttribute("student", matchedStudent);
                } else {
                    request.setAttribute("searchError", "USN number is not registered in the system database.");
                }
            }
        } catch (SQLException e) {
            log("Database exception while fetching allocation for USN " + cleanUsn, e);
            request.setAttribute("searchError", "Unable to fetch allocation now. Please try again later.");
        }
    }

    private Student toStudent(String usn, ResultSet rs) throws SQLException {
        String name = rs.getString("Name");
        String email = rs.getString("Email");
        Student student = new Student(usn, name, isBlank(email) ? "No Email Provided" : email);

        String room = rs.getString("RoomID");
        if (isBlank(room)) {
            student.setAllocatedRoom("Not Allocated yet");
            student.setSeatNumber(0);
        } else {
            student.setAllocatedRoom(room);
            student.setSeatNumber(rs.getInt("BenchNumber"));
            student.setSeatPosition(rs.getInt("SeatPosition"));
        }
        student.setExamSubject(rs.getString("Subject"));
        student.setExamDate(rs.getString("ExamDate"));
        student.setExamTime(rs.getString("ExamTime"));

        return student;
    }

    private List<Student> fetchAllAllocationsFromDB() {
        List<Student> list = new ArrayList<>();
        String sqlQuery = "SELECT s.USN, s.Name, s.Email, a.RoomID, a.BenchNumber "
                + "FROM Students s "
                + "JOIN Seat_Allocations a ON s.USN = a.USN";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String email = rs.getString("Email");
                Student student = new Student(
                        rs.getString("USN"),
                        rs.getString("Name"),
                        isBlank(email) ? "No Email Provided" : email
                );
                student.setAllocatedRoom(rs.getString("RoomID"));
                student.setSeatNumber(rs.getInt("BenchNumber"));
                list.add(student);
            }
        } catch (SQLException e) {
            log("Database connection failure while listing allocations", e);
        }

        return list;
    }

    private boolean isAdminLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"));
    }

    private void loadSearchOptions(HttpServletRequest request) {
        try {
            DBUtil.ensureSchema();
            request.setAttribute("exams", fetchExams());
            request.setAttribute("selectedExamId", selectedExamId(request));
        } catch (SQLException e) {
            log("Unable to load exam options", e);
            request.setAttribute("searchError", "Unable to load exam sessions.");
        }
    }

    private List<AdminServlet.ExamOption> fetchExams() throws SQLException {
        List<AdminServlet.ExamOption> exams = new ArrayList<>();
        String sql = "SELECT ExamID, Subject, ExamDate, ExamTime, Semester FROM Exams ORDER BY ExamDate DESC, ExamID DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                exams.add(new AdminServlet.ExamOption(
                        rs.getInt("ExamID"),
                        rs.getString("Subject"),
                        String.valueOf(rs.getDate("ExamDate")),
                        rs.getString("ExamTime"),
                        rs.getInt("Semester")
                ));
            }
        }
        return exams;
    }

    private int selectedExamId(HttpServletRequest request) {
        int requested = parseInt(request.getParameter("examId"), 0);
        if (requested > 0) {
            return requested;
        }
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(ExamID), 0) FROM Exams");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return isBlank(value) ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Properties loadMailConfig() throws IOException {
        Properties properties = new Properties();
        try (var input = getServletContext().getResourceAsStream("/WEB-INF/config.properties")) {
            if (input == null) {
                throw new IOException("WEB-INF/config.properties not found");
            }
            properties.load(input);
        }
        return properties;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
