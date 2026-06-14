<%@ page import="java.util.List" %>
<%@ page import="com.exam.locator.AdminServlet" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<AdminServlet.ExamOption> exams = (List<AdminServlet.ExamOption>) request.getAttribute("exams");
    int selectedExamId = request.getAttribute("selectedExamId") == null ? 0 : (Integer) request.getAttribute("selectedExamId");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Exam Hall Locator | Result</title>
    <style>
        :root { --bg: #eef2f6; --surface: #ffffff; --line: #d8e0ea; --text: #172033; --muted: #657084; --primary: #1d4ed8; --success: #0f766e; --danger: #b91c1c; }
        * { box-sizing: border-box; }
        body { font-family: "Segoe UI", Arial, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 22px; background: var(--bg); color: var(--text); }
        body:before { content: ""; position: fixed; top: 0; left: 0; right: 0; height: 5px; background: linear-gradient(90deg, #1d4ed8, #0f766e, #b45309); }
        .container { width: 100%; max-width: 560px; background: var(--surface); border: 1px solid var(--line); border-radius: 8px; box-shadow: 0 16px 34px rgba(23, 32, 51, 0.09); padding: 34px; }
        h1 { margin: 0 0 8px; text-align: center; font-size: 27px; line-height: 1.2; }
        .subtitle { margin: 0 0 24px; text-align: center; color: var(--muted); font-size: 14px; }
        label { display: block; margin: 14px 0 6px; font-size: 14px; font-weight: 750; color: #334155; }
        input, select { width: 100%; padding: 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 15px; background: #fff; color: var(--text); }
        input:focus, select:focus { outline: 3px solid rgba(29, 78, 216, 0.16); border-color: var(--primary); }
        button, .button-link { display: inline-flex; align-items: center; justify-content: center; width: 100%; margin-top: 16px; padding: 12px; border: 0; border-radius: 6px; background: var(--primary); color: #fff; font-size: 15px; font-weight: 750; cursor: pointer; text-align: center; text-decoration: none; }
        .button-link { background: var(--success); }
        .result-box { border: 1px solid #bbf7d0; border-left: 5px solid var(--success); background: #f0fdf4; border-radius: 8px; padding: 20px; }
        .result-box h2 { margin: 0 0 14px; color: #166534; font-size: 21px; }
        .result-box p { display: flex; justify-content: space-between; gap: 16px; margin: 10px 0; border-bottom: 1px solid rgba(22, 101, 52, 0.12); padding-bottom: 8px; }
        .result-box p:last-child { border-bottom: 0; padding-bottom: 0; }
        .error { margin-top: 16px; padding: 11px 13px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; color: var(--danger); font-size: 14px; font-weight: 700; }
        .links { display: flex; justify-content: center; gap: 16px; margin-top: 20px; flex-wrap: wrap; }
        .links a { color: var(--primary); text-decoration: none; font-size: 14px; font-weight: 750; }
        .muted { color: var(--muted); font-size: 13px; }
        @media (max-width: 560px) {
            .container { padding: 24px; }
            .result-box p { display: block; }
        }
    </style>
</head>
<body>
<main class="container">
    <% if (request.getAttribute("student") != null) {
        com.exam.locator.Student s = (com.exam.locator.Student) request.getAttribute("student");
        boolean allocated = s.getAllocatedRoom() != null && !s.getAllocatedRoom().contains("Not Allocated");
    %>
    <section class="result-box">
        <h2>Allocation Details</h2>
        <p><strong>Name:</strong> <%= s.getName() %></p>
        <p><strong>USN:</strong> <%= s.getUsn() %></p>
        <p><strong>Subject:</strong> <%= s.getExamSubject() == null ? "Selected exam" : s.getExamSubject() %></p>
        <p><strong>Date / Time:</strong> <%= s.getExamDate() == null ? "-" : s.getExamDate() %> <%= s.getExamTime() == null ? "" : s.getExamTime() %></p>
        <p><strong>Allocated Room:</strong> <%= s.getAllocatedRoom() %></p>
        <p><strong>Bench / Seat:</strong> <%= s.getSeatNumber() %> / <%= s.getSeatPosition() %></p>
    </section>
    <% if (allocated) { %>
    <a class="button-link" href="${pageContext.request.contextPath}/hall-ticket?usn=<%= s.getUsn() %>&examId=<%= selectedExamId %>">Download hall ticket</a>
    <% } else { %>
    <div class="error">This USN is registered, but no seat has been saved for the selected exam yet.</div>
    <% } %>
    <div class="links">
        <a href="${pageContext.request.contextPath}/allocate">Search another USN</a>
        <a href="${pageContext.request.contextPath}/login.jsp">Admin login</a>
    </div>
    <% } else { %>
    <h1>Find Your Exam Hall</h1>
    <p class="subtitle">Select the exam session and enter your USN to view the saved seating details.</p>
    <form action="${pageContext.request.contextPath}/allocate" method="post">
        <label for="examId">Exam session</label>
        <select id="examId" name="examId" required>
            <% if (exams == null || exams.isEmpty()) { %>
            <option value="0">No exam sessions available</option>
            <% } else {
                for (AdminServlet.ExamOption exam : exams) {
            %>
            <option value="<%= exam.getExamId() %>" <%= exam.getExamId() == selectedExamId ? "selected" : "" %>>
                <%= exam.getSubject() %> | <%= exam.getExamDate() %> <%= exam.getExamTime() %> | Sem <%= exam.getSemester() %>
            </option>
            <% }} %>
        </select>

        <label for="usn">USN</label>
        <input type="text" id="usn" name="usn" placeholder="Enter your USN" required autofocus autocomplete="off">
        <button type="submit">Search Location</button>
    </form>

    <% if (request.getAttribute("searchError") != null) { %>
    <div class="error"><%= request.getAttribute("searchError") %></div>
    <% } %>

    <div class="links">
        <a href="${pageContext.request.contextPath}/login.jsp">Admin login</a>
    </div>
    <% } %>
</main>
</body>
</html>
