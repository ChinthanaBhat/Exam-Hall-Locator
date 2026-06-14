package com.exam.locator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@WebServlet("/admin")
@MultipartConfig
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadDashboard(request);
        request.getRequestDispatcher("admin.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        try {
            DBUtil.ensureSchema();

            if ("createExam".equals(action)) {
                createExam(request);
                request.setAttribute("statusMessage", "Exam session created.");
            } else if ("importStudents".equals(action)) {
                int count = importStudents(request.getPart("studentsFile"));
                request.setAttribute("statusMessage", count + " student records imported.");
            } else if ("importRooms".equals(action)) {
                int count = importRooms(request.getPart("roomsFile"));
                request.setAttribute("statusMessage", count + " room records imported.");
            } else if ("previewAllocation".equals(action)) {
                request.setAttribute("previewRows", buildAllocationPreview(selectedExamId(request)));
                request.setAttribute("statusMessage", "Preview generated. Review it before saving.");
            } else if ("saveAllocation".equals(action)) {
                int examId = requiredExamId(request);
                List<AllocationPreviewRow> preview = buildAllocationPreview(examId);
                saveAllocation(examId, preview);
                List<AllocationPreviewRow> savedRows = fetchSavedAllocationRows(examId);
                request.setAttribute("previewRows", savedRows);
                request.setAttribute("statusMessage", savedRows.size() + " allocations saved.");
            } else if ("clearAllocations".equals(action)) {
                int deleted = clearAllocations(requiredExamId(request));
                request.setAttribute("statusMessage", deleted + " allocations cleared.");
            } else if ("sendEmails".equals(action)) {
                List<Student> activeAllocations = fetchAllAllocations(requiredExamId(request));
                EmailUtil.sendAllocationEmails(activeAllocations, loadMailConfig());
                request.setAttribute("emailStatus", "Email broadcast started. Keep this page open to watch progress.");
            }
        } catch (Exception e) {
            log("Admin action failed", e);
            request.setAttribute("errorMessage", e.getMessage());
        }

        loadDashboard(request);
        request.getRequestDispatcher("admin.jsp").forward(request, response);
    }

    private void loadDashboard(HttpServletRequest request) {
        try {
            DBUtil.ensureSchema();
            int selectedExamId = selectedExamId(request);
            request.setAttribute("exams", fetchExams());
            request.setAttribute("selectedExamId", selectedExamId);
            DashboardStats stats = fetchStats(selectedExamId);
            request.setAttribute("stats", stats);
            request.setAttribute("roomUsage", fetchRoomUsage(selectedExamId));
            if (request.getAttribute("previewRows") == null && stats.getAllocatedStudents() > 0) {
                request.setAttribute("previewRows", fetchSavedAllocationRows(selectedExamId));
            }
        } catch (SQLException e) {
            log("Dashboard load failed", e);
            request.setAttribute("exams", new ArrayList<ExamOption>());
            request.setAttribute("selectedExamId", 0);
            request.setAttribute("stats", new DashboardStats(0, 0, 0));
            request.setAttribute("roomUsage", new ArrayList<RoomUsage>());
            request.setAttribute("errorMessage", "Unable to load dashboard: " + e.getMessage());
        }
    }

    private void createExam(HttpServletRequest request) throws SQLException {
        String subject = required(request, "subject");
        String examDate = required(request, "examDate");
        String examTime = required(request, "examTime");
        int semester = parseInt(request.getParameter("semester"), 0);

        String sql = "INSERT INTO Exams (Subject, ExamDate, ExamTime, Semester) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject);
            ps.setDate(2, Date.valueOf(examDate));
            ps.setString(3, examTime);
            ps.setInt(4, semester);
            ps.executeUpdate();
        }
    }

    private int importStudents(Part part) throws IOException, SQLException {
        if (part == null || part.getSize() == 0) {
            throw new IllegalArgumentException("Choose a students CSV file.");
        }

        int count = 0;
        try (Connection conn = DBUtil.getConnection();
             BufferedReader reader = new BufferedReader(new InputStreamReader(part.getInputStream(), StandardCharsets.UTF_8));
             PreparedStatement ps = conn.prepareStatement("INSERT INTO Students (USN, Name, Email, Department, Semester) "
                     + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                     + "Name = VALUES(Name), Email = VALUES(Email), Department = VALUES(Department), Semester = VALUES(Semester)")) {

            List<String> headers = parseCsvLine(reader.readLine());
            Map<String, Integer> index = headerIndex(headers);
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = parseCsvLine(line);
                String usn = value(row, index, "usn", "usn ");
                String name = value(row, index, "student name", "name");
                if (isBlank(usn) || isBlank(name)) {
                    continue;
                }
                ps.setString(1, usn.trim().toUpperCase());
                ps.setString(2, name.trim());
                ps.setString(3, value(row, index, "email"));
                ps.setString(4, firstNonBlank(value(row, index, "department"), deriveDepartment(usn)));
                ps.setInt(5, parseInt(value(row, index, "semester"), 0));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        return count;
    }

    private int importRooms(Part part) throws IOException, SQLException {
        if (part == null || part.getSize() == 0) {
            throw new IllegalArgumentException("Choose a rooms CSV file.");
        }

        int count = 0;
        try (Connection conn = DBUtil.getConnection();
             BufferedReader reader = new BufferedReader(new InputStreamReader(part.getInputStream(), StandardCharsets.UTF_8));
             PreparedStatement ps = conn.prepareStatement("INSERT INTO Rooms (RoomID, Capacity, Floor) "
                     + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE Capacity = VALUES(Capacity), Floor = VALUES(Floor)")) {

            List<String> headers = parseCsvLine(reader.readLine());
            Map<String, Integer> index = headerIndex(headers);
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = parseCsvLine(line);
                String roomId = value(row, index, "roomnumber", "roomid", "room");
                int capacity = parseInt(value(row, index, "capacity"), 0);
                if (isBlank(roomId) || capacity <= 0) {
                    continue;
                }
                ps.setString(1, roomId.trim());
                ps.setInt(2, capacity);
                ps.setString(3, value(row, index, "floor"));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        return count;
    }

    private List<AllocationPreviewRow> buildAllocationPreview(int examId) throws SQLException {
        ExamOption exam = fetchExam(examId);
        if (exam == null) {
            throw new IllegalArgumentException("Create or select an exam session first.");
        }

        List<Student> students = fetchStudentsForExam(exam);
        List<Room> rooms = fetchRooms();
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("No rooms found. Upload rooms.csv first and confirm Room Capacity Usage shows rooms.");
        }
        int totalCapacity = rooms.stream().mapToInt(Room::getCapacity).sum();
        if (students.size() > totalCapacity) {
            throw new IllegalArgumentException("Room capacity is short by " + (students.size() - totalCapacity) + " seats.");
        }

        List<AllocationPreviewRow> preview = new ArrayList<>();
        int roomIndex = 0;
        int usedInRoom = 0;
        String activeDepartment = "";

        for (Student student : students) {
            String department = normalDepartment(student.getDepartment());
            int remainingInGroup = countRemainingDepartment(students, preview.size(), department);
            if (!department.equals(activeDepartment) && roomIndex < rooms.size()) {
                int remainingInRoom = rooms.get(roomIndex).getCapacity() - usedInRoom;
                if (usedInRoom > 0 && remainingInGroup > remainingInRoom && hasAnotherRoom(rooms, roomIndex)) {
                    roomIndex++;
                    usedInRoom = 0;
                }
                activeDepartment = department;
            }

            while (roomIndex < rooms.size() && usedInRoom >= rooms.get(roomIndex).getCapacity()) {
                roomIndex++;
                usedInRoom = 0;
            }
            if (roomIndex >= rooms.size()) {
                throw new IllegalArgumentException("Not enough room capacity for all students.");
            }

            Room room = rooms.get(roomIndex);
            int bench = (usedInRoom / 2) + 1;
            int seatPosition = (usedInRoom % 2) + 1;
            preview.add(new AllocationPreviewRow(student.getUsn(), student.getName(), department,
                    room.getName(), bench, seatPosition));
            usedInRoom++;
        }

        return preview;
    }

    private void saveAllocation(int examId, List<AllocationPreviewRow> preview) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);

            // Wrap the execution statement inside index tracking safety wrappers
            try (Statement safetyStmt = conn.createStatement();
                 PreparedStatement delete = conn.prepareStatement("DELETE FROM Seat_Allocations WHERE ExamID = ?");
                 PreparedStatement insert = conn.prepareStatement("INSERT INTO Seat_Allocations "
                         + "(ExamID, USN, RoomID, BenchNumber, SeatPosition) VALUES (?, ?, ?, ?, ?) "
                         + "ON DUPLICATE KEY UPDATE RoomID = VALUES(RoomID), "
                         + "BenchNumber = VALUES(BenchNumber), SeatPosition = VALUES(SeatPosition)")) {

                // Turn off tracking locks
                safetyStmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");

                delete.setInt(1, examId);
                delete.executeUpdate();

                for (AllocationPreviewRow row : preview) {
                    insert.setInt(1, examId);
                    insert.setString(2, row.getUsn());
                    insert.setString(3, row.getRoomId());
                    insert.setInt(4, row.getBenchNumber());
                    insert.setInt(5, row.getSeatPosition());
                    insert.addBatch();
                }
                insert.executeBatch();

                // Restore constraints before committing
                safetyStmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private int clearAllocations(int examId) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM Seat_Allocations WHERE ExamID = ?")) {
            ps.setInt(1, examId);
            return ps.executeUpdate();
        }
    }

    private DashboardStats fetchStats(int examId) throws SQLException {
        int total = countEligibleStudents(examId);
        int allocated = 0;
        if (examId > 0) {
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COUNT(DISTINCT USN) FROM Seat_Allocations WHERE ExamID = ?")) {
                ps.setInt(1, examId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        allocated = rs.getInt(1);
                    }
                }
            }
        }
        return new DashboardStats(total, allocated, Math.max(0, total - allocated));
    }

    private int countEligibleStudents(int examId) throws SQLException {
        ExamOption exam = examId > 0 ? fetchExam(examId) : null;
        if (exam == null || exam.getSemester() <= 0) {
            return scalar("SELECT COUNT(*) FROM Students");
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Students WHERE Semester = ? OR Semester = 0")) {
            ps.setInt(1, exam.getSemester());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private List<RoomUsage> fetchRoomUsage(int examId) throws SQLException {
        List<RoomUsage> list = new ArrayList<>();
        String sql = "SELECT r.RoomID, r.Capacity, COUNT(a.USN) AS UsedSeats "
                + "FROM Rooms r LEFT JOIN Seat_Allocations a ON r.RoomID = a.RoomID AND a.ExamID = ? "
                + "GROUP BY r.RoomID, r.Capacity ORDER BY r.RoomID";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RoomUsage(rs.getString("RoomID"), rs.getInt("Capacity"), rs.getInt("UsedSeats")));
                }
            }
        }
        return list;
    }

    private List<ExamOption> fetchExams() throws SQLException {
        List<ExamOption> exams = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ExamID, Subject, ExamDate, ExamTime, Semester FROM Exams ORDER BY ExamDate DESC, ExamID DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                exams.add(toExam(rs));
            }
        }
        return exams;
    }

    private ExamOption fetchExam(int examId) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ExamID, Subject, ExamDate, ExamTime, Semester FROM Exams WHERE ExamID = ?")) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toExam(rs) : null;
            }
        }
    }

    private List<Student> fetchStudentsForExam(ExamOption exam) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT USN, Name, Email, Department, Semester FROM Students "
                + (exam.getSemester() > 0 ? "WHERE Semester = ? OR Semester = 0 " : "")
                + "ORDER BY COALESCE(NULLIF(Department, ''), 'ZZZ'), USN";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (exam.getSemester() > 0) {
                ps.setInt(1, exam.getSemester());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Student student = new Student(rs.getString("USN"), rs.getString("Name"), rs.getString("Email"));
                    student.setDepartment(firstNonBlank(rs.getString("Department"), deriveDepartment(student.getUsn())));
                    student.setSemester(rs.getInt("Semester"));
                    students.add(student);
                }
            }
        }
        return students;
    }

    private List<Room> fetchRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT RoomID, Capacity, Floor FROM Rooms ORDER BY RoomID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Room room = new Room(rs.getString("RoomID"), rs.getInt("Capacity"));
                room.setFloor(rs.getString("Floor"));
                rooms.add(room);
            }
        }
        return rooms;
    }

    private List<Student> fetchAllAllocations(int examId) throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT s.USN, s.Name, s.Email, a.RoomID, a.BenchNumber, a.SeatPosition, e.Subject, e.ExamDate, e.ExamTime "
                + "FROM Students s JOIN Seat_Allocations a ON s.USN = a.USN "
                + "JOIN Exams e ON a.ExamID = e.ExamID WHERE a.ExamID = ? ORDER BY s.USN";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Student student = new Student(rs.getString("USN"), rs.getString("Name"), rs.getString("Email"));
                    student.setAllocatedRoom(rs.getString("RoomID"));
                    student.setSeatNumber(rs.getInt("BenchNumber"));
                    student.setSeatPosition(rs.getInt("SeatPosition"));
                    student.setExamSubject(rs.getString("Subject"));
                    student.setExamDate(String.valueOf(rs.getDate("ExamDate")));
                    student.setExamTime(rs.getString("ExamTime"));
                    list.add(student);
                }
            }
        }
        return list;
    }

    private List<AllocationPreviewRow> fetchSavedAllocationRows(int examId) throws SQLException {
        List<AllocationPreviewRow> rows = new ArrayList<>();
        String sql = "SELECT s.USN, s.Name, COALESCE(NULLIF(s.Department, ''), 'GENERAL') AS Department, "
                + "a.RoomID, a.BenchNumber, a.SeatPosition "
                + "FROM Seat_Allocations a JOIN Students s ON s.USN = a.USN "
                + "WHERE a.ExamID = ? ORDER BY a.RoomID, a.BenchNumber, a.SeatPosition, s.USN";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new AllocationPreviewRow(
                            rs.getString("USN"),
                            rs.getString("Name"),
                            normalDepartment(rs.getString("Department")),
                            rs.getString("RoomID"),
                            rs.getInt("BenchNumber"),
                            rs.getInt("SeatPosition")
                    ));
                }
            }
        }
        return rows;
    }

    private int selectedExamId(HttpServletRequest request) {
        int requested = parseInt(request.getParameter("examId"), 0);
        if (requested > 0) {
            return requested;
        }
        try {
            return scalar("SELECT COALESCE(MAX(ExamID), 0) FROM Exams");
        } catch (SQLException e) {
            return 0;
        }
    }

    private int requiredExamId(HttpServletRequest request) {
        int examId = selectedExamId(request);
        if (examId <= 0) {
            throw new IllegalArgumentException("Create or select an exam session first.");
        }
        return examId;
    }

    private int scalar(String sql) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
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

    private ExamOption toExam(ResultSet rs) throws SQLException {
        return new ExamOption(rs.getInt("ExamID"), rs.getString("Subject"),
                String.valueOf(rs.getDate("ExamDate")), rs.getString("ExamTime"), rs.getInt("Semester"));
    }

    private int countRemainingDepartment(List<Student> students, int start, String department) {
        int count = 0;
        for (int i = start; i < students.size(); i++) {
            if (department.equals(normalDepartment(students.get(i).getDepartment()))) {
                count++;
            }
        }
        return count;
    }

    private boolean hasAnotherRoom(List<Room> rooms, int roomIndex) {
        return roomIndex + 1 < rooms.size();
    }

    private String deriveDepartment(String usn) {
        if (usn == null || usn.length() < 7) {
            return "GENERAL";
        }
        return usn.substring(5, Math.min(7, usn.length())).toUpperCase();
    }

    private String normalDepartment(String value) {
        return firstNonBlank(value, "GENERAL").trim().toUpperCase();
    }

    private String required(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (isBlank(value)) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value.trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private Map<String, Integer> headerIndex(List<String> headers) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(headers.get(i).trim().toLowerCase(), i);
        }
        return index;
    }

    private String value(List<String> row, Map<String, Integer> index, String... names) {
        for (String name : names) {
            Integer position = index.get(name.toLowerCase());
            if (position != null && position < row.size()) {
                return row.get(position);
            }
        }
        return "";
    }

    private int parseInt(String value, int fallback) {
        try {
            return isBlank(value) ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class DashboardStats {
        private final int totalStudents;
        private final int allocatedStudents;
        private final int unallocatedStudents;

        DashboardStats(int totalStudents, int allocatedStudents, int unallocatedStudents) {
            this.totalStudents = totalStudents;
            this.allocatedStudents = allocatedStudents;
            this.unallocatedStudents = unallocatedStudents;
        }

        public int getTotalStudents() { return totalStudents; }
        public int getAllocatedStudents() { return allocatedStudents; }
        public int getUnallocatedStudents() { return unallocatedStudents; }
    }

    public static class RoomUsage {
        private final String roomId;
        private final int capacity;
        private final int usedSeats;

        RoomUsage(String roomId, int capacity, int usedSeats) {
            this.roomId = roomId;
            this.capacity = capacity;
            this.usedSeats = usedSeats;
        }

        public String getRoomId() { return roomId; }
        public int getCapacity() { return capacity; }
        public int getUsedSeats() { return usedSeats; }
    }

    public static class ExamOption {
        private final int examId;
        private final String subject;
        private final String examDate;
        private final String examTime;
        private final int semester;

        ExamOption(int examId, String subject, String examDate, String examTime, int semester) {
            this.examId = examId;
            this.subject = subject;
            this.examDate = examDate;
            this.examTime = examTime;
            this.semester = semester;
        }

        public int getExamId() { return examId; }
        public String getSubject() { return subject; }
        public String getExamDate() { return examDate; }
        public String getExamTime() { return examTime; }
        public int getSemester() { return semester; }
    }

    public static class AllocationPreviewRow {
        private final String usn;
        private final String name;
        private final String department;
        private final String roomId;
        private final int benchNumber;
        private final int seatPosition;

        AllocationPreviewRow(String usn, String name, String department, String roomId, int benchNumber, int seatPosition) {
            this.usn = usn;
            this.name = name;
            this.department = department;
            this.roomId = roomId;
            this.benchNumber = benchNumber;
            this.seatPosition = seatPosition;
        }

        public String getUsn() { return usn; }
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public String getRoomId() { return roomId; }
        public int getBenchNumber() { return benchNumber; }
        public int getSeatPosition() { return seatPosition; }
    }
}
