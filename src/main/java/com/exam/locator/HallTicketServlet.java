package com.exam.locator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/hall-ticket")
public class HallTicketServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String usn = request.getParameter("usn");
        int examId = parseInt(request.getParameter("examId"), 0);
        if (isBlank(usn) || examId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "USN and examId are required.");
            return;
        }

        try {
            DBUtil.ensureSchema();
            Student student = fetchTicket(usn.trim().toUpperCase(), examId);
            if (student == null || student.getAllocatedRoom() == null || student.getAllocatedRoom().contains("Not Allocated")) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No saved allocation found for this exam.");
                return;
            }

            byte[] pdf = buildPdf(student);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=hall-ticket-" + student.getUsn() + ".pdf");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
        } catch (SQLException e) {
            log("Unable to generate hall ticket", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to generate hall ticket.");
        }
    }

    private Student fetchTicket(String usn, int examId) throws SQLException {
        String sql = "SELECT s.USN, s.Name, s.Email, a.RoomID, a.BenchNumber, a.SeatPosition, "
                + "e.Subject, e.ExamDate, e.ExamTime "
                + "FROM Students s JOIN Seat_Allocations a ON s.USN = a.USN "
                + "JOIN Exams e ON a.ExamID = e.ExamID "
                + "WHERE s.USN = ? AND a.ExamID = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usn);
            ps.setInt(2, examId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Student student = new Student(rs.getString("USN"), rs.getString("Name"), rs.getString("Email"));
                student.setAllocatedRoom(rs.getString("RoomID"));
                student.setSeatNumber(rs.getInt("BenchNumber"));
                student.setSeatPosition(rs.getInt("SeatPosition"));
                student.setExamSubject(rs.getString("Subject"));
                student.setExamDate(String.valueOf(rs.getDate("ExamDate")));
                student.setExamTime(rs.getString("ExamTime"));
                return student;
            }
        }
    }

    private byte[] buildPdf(Student student) throws IOException {
        List<String> lines = List.of(
                "Exam Hall Ticket",
                "USN: " + student.getUsn(),
                "Name: " + student.getName(),
                "Subject: " + safe(student.getExamSubject()),
                "Exam Date: " + safe(student.getExamDate()),
                "Exam Time: " + safe(student.getExamTime()),
                "Room: " + student.getAllocatedRoom(),
                "Bench: " + student.getSeatNumber() + "   Seat: " + student.getSeatPosition(),
                "Instructions:",
                "1. Carry college ID and this hall ticket.",
                "2. Report to the exam hall at least 30 minutes early.",
                "3. Mobile phones and unauthorized materials are not allowed."
        );

        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 20 Tf\n72 760 Td\n(").append(pdfEscape(lines.get(0))).append(") Tj\n");
        content.append("/F1 12 Tf\n0 -34 Td\n");
        for (int i = 1; i < lines.size(); i++) {
            content.append("(").append(pdfEscape(lines.get(i))).append(") Tj\n0 -22 Td\n");
        }
        content.append("ET");

        List<byte[]> objects = new ArrayList<>();
        objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        objects.add(("5 0 obj\n<< /Length " + stream.length + " >>\nstream\n" + content + "\nendstream\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (byte[] object : objects) {
            offsets.add(out.size());
            out.write(object);
        }

        int xref = out.size();
        out.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
        out.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 1; i < offsets.size(); i++) {
            out.write(String.format("%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.ISO_8859_1));
        }
        out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
                + xref + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }

    private String pdfEscape(String value) {
        return safe(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int parseInt(String value, int fallback) {
        try {
            return isBlank(value) ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
