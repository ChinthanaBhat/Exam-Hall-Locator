<%@ page import="java.util.List" %>
<%@ page import="com.exam.locator.AdminServlet" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    AdminServlet.DashboardStats stats = (AdminServlet.DashboardStats) request.getAttribute("stats");
    List<AdminServlet.RoomUsage> roomUsage = (List<AdminServlet.RoomUsage>) request.getAttribute("roomUsage");
    List<AdminServlet.ExamOption> exams = (List<AdminServlet.ExamOption>) request.getAttribute("exams");
    List<AdminServlet.AllocationPreviewRow> previewRows = (List<AdminServlet.AllocationPreviewRow>) request.getAttribute("previewRows");
    int selectedExamId = request.getAttribute("selectedExamId") == null ? 0 : (Integer) request.getAttribute("selectedExamId");
    int totalStudents = stats == null ? 0 : stats.getTotalStudents();
    int allocatedStudents = stats == null ? 0 : stats.getAllocatedStudents();
    int unallocatedStudents = stats == null ? 0 : stats.getUnallocatedStudents();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Admin Dashboard | Exam Hall Locator</title>
    <style>
        :root {
            --bg: #eef2f6;
            --surface: #ffffff;
            --line: #d8e0ea;
            --line-soft: #e7edf4;
            --text: #172033;
            --muted: #657084;
            --primary: #1d4ed8;
            --primary-dark: #1e40af;
            --success: #0f766e;
            --danger: #b91c1c;
            --warning: #b45309;
        }
        * { box-sizing: border-box; }
        body { margin: 0; font-family: "Segoe UI", Arial, sans-serif; background: var(--bg); color: var(--text); }
        body:before { content: ""; display: block; height: 5px; background: linear-gradient(90deg, #1d4ed8, #0f766e, #b45309); }
        .shell { max-width: 1220px; margin: 0 auto; padding: 28px 20px 42px; }
        .topbar, .actions, .inline { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
        .topbar { justify-content: space-between; margin-bottom: 18px; }
        h1 { margin: 0; font-size: 30px; line-height: 1.15; }
        h2 { margin: 0; font-size: 18px; }
        .section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; }
        .muted { color: var(--muted); font-size: 13px; }
        .panel, .card { background: var(--surface); border: 1px solid var(--line); border-radius: 8px; box-shadow: 0 12px 28px rgba(23, 32, 51, 0.06); }
        .panel { padding: 20px; margin-bottom: 18px; }
        .cards { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-bottom: 18px; }
        .card { padding: 18px; border-top: 3px solid var(--primary); }
        .card:nth-child(2) { border-top-color: var(--success); }
        .card:nth-child(3) { border-top-color: var(--warning); }
        .metric { font-size: 34px; line-height: 1; font-weight: 800; margin-top: 8px; }
        label { display: block; margin-bottom: 6px; font-weight: 700; font-size: 13px; color: #334155; }
        input, select {
            min-height: 40px; padding: 9px 11px; border: 1px solid #c9d2df; border-radius: 6px;
            background: #fff; color: var(--text); font: inherit;
        }
        input:focus, select:focus { outline: 3px solid rgba(29, 78, 216, 0.16); border-color: var(--primary); }
        input[type="file"] { padding: 7px; max-width: 270px; }
        button, .link-button {
            border: 0; border-radius: 6px; padding: 10px 14px; font-weight: 750; cursor: pointer;
            font-size: 14px; text-decoration: none; min-height: 40px; display: inline-flex; align-items: center; justify-content: center;
        }
        button:hover, .link-button:hover { filter: brightness(0.97); }
        .primary { background: var(--primary); color: #fff; }
        .secondary { background: #e7edf4; color: var(--text); border: 1px solid #cfd8e3; }
        .danger { background: var(--danger); color: #fff; }
        .success { background: var(--success); color: #fff; }
        .status, .error { margin-bottom: 18px; padding: 12px 14px; border-radius: 8px; font-weight: 700; border: 1px solid; }
        .status { background: #ecfdf5; color: #166534; border-color: #bbf7d0; }
        .error { background: #fef2f2; color: var(--danger); border-color: #fecaca; }
        .table-wrap { overflow-x: auto; border: 1px solid var(--line-soft); border-radius: 8px; }
        table { width: 100%; border-collapse: collapse; font-size: 14px; background: #fff; }
        th, td { padding: 11px 12px; border-bottom: 1px solid var(--line-soft); text-align: left; white-space: nowrap; }
        th { color: #475569; font-size: 12px; text-transform: uppercase; background: #f8fafc; }
        tbody tr:hover { background: #f8fafc; }
        .grid-2 { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18px; }
        .room-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 12px; }
        .usage { padding: 13px; border: 1px solid var(--line-soft); border-radius: 8px; background: #fbfcfe; }
        .usage strong { display: flex; justify-content: space-between; gap: 12px; font-size: 14px; }
        .bar-wrap { height: 9px; background: #e7edf4; border-radius: 999px; overflow: hidden; margin-top: 10px; }
        .bar { height: 100%; background: var(--primary); }
        .progress-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
        .column { min-height: 170px; border: 1px solid var(--line-soft); border-radius: 8px; background: #fbfcfe; padding: 12px; }
        .student-badge { border-left: 4px solid #9ca3af; background: #fff; border-radius: 6px; padding: 8px 10px; margin-bottom: 8px; font-size: 13px; }
        .exam-select { min-width: min(680px, 100%); }
        .form-stack { align-items: flex-end; }
        @media (max-width: 820px) {
            .cards, .grid-2, .progress-grid { grid-template-columns: 1fr; }
            .topbar { align-items: flex-start; }
            input, select, button, .link-button { width: 100%; }
            .actions form, .inline > div { width: 100%; }
        }
    </style>
</head>
<body>
<main class="shell">
    <div class="topbar">
        <div>
            <h1>Admin Dashboard</h1>
            <div class="muted">Manage exam sessions, import data, allocate seats, and notify students.</div>
        </div>
        <div class="actions">
            <a class="link-button secondary" href="${pageContext.request.contextPath}/allocate">Student Search</a>
            <form action="${pageContext.request.contextPath}/allocate" method="post">
                <input type="hidden" name="action" value="logout">
                <button class="danger" type="submit">Logout</button>
            </form>
        </div>
    </div>

    <% if (request.getAttribute("statusMessage") != null) { %>
    <div class="status"><%= request.getAttribute("statusMessage") %></div>
    <% } %>
    <% if (request.getAttribute("emailStatus") != null) { %>
    <div class="status"><%= request.getAttribute("emailStatus") %></div>
    <% } %>
    <% if (request.getAttribute("errorMessage") != null) { %>
    <div class="error"><%= request.getAttribute("errorMessage") %></div>
    <% } %>

    <section class="panel">
        <div class="section-head">
            <h2>Active Session</h2>
            <span class="muted">All allocation actions use this exam</span>
        </div>
        <form class="inline" action="${pageContext.request.contextPath}/admin" method="get">
            <div>
                <label for="examId">Active exam</label>
                <select class="exam-select" id="examId" name="examId" onchange="this.form.submit()">
                    <% if (exams == null || exams.isEmpty()) { %>
                    <option value="0">No exam sessions</option>
                    <% } else {
                        for (AdminServlet.ExamOption exam : exams) {
                    %>
                    <option value="<%= exam.getExamId() %>" <%= exam.getExamId() == selectedExamId ? "selected" : "" %>>
                        <%= exam.getSubject() %> | <%= exam.getExamDate() %> <%= exam.getExamTime() %> | Sem <%= exam.getSemester() %>
                    </option>
                    <% }} %>
                </select>
            </div>
            <button class="secondary" type="submit">Load</button>
        </form>
    </section>

    <section class="cards">
        <div class="card"><div class="muted">Total students</div><div class="metric"><%= totalStudents %></div></div>
        <div class="card"><div class="muted">Allocated</div><div class="metric"><%= allocatedStudents %></div></div>
        <div class="card"><div class="muted">Unallocated</div><div class="metric"><%= unallocatedStudents %></div></div>
    </section>

    <section class="grid-2">
        <div class="panel">
            <div class="section-head">
                <h2>Create Exam Session</h2>
            </div>
            <form class="inline form-stack" action="${pageContext.request.contextPath}/admin" method="post">
                <input type="hidden" name="action" value="createExam">
                <div><label>Subject</label><input name="subject" required></div>
                <div><label>Date</label><input type="date" name="examDate" required></div>
                <div><label>Time</label><input name="examTime" placeholder="10:00 AM - 1:00 PM" required></div>
                <div><label>Semester</label><input type="number" min="0" name="semester" value="0"></div>
                <button class="primary" type="submit">Create</button>
            </form>
        </div>

        <div class="panel">
            <div class="section-head">
                <h2>CSV Import</h2>
                <span class="muted">CSV exported from Excel works</span>
            </div>
            <form class="inline" action="${pageContext.request.contextPath}/admin" method="post" enctype="multipart/form-data">
                <input type="hidden" name="action" value="importStudents">
                <input type="file" name="studentsFile" accept=".csv,text/csv" required>
                <button class="secondary" type="submit">Upload students.csv</button>
            </form>
            <br>
            <form class="inline" action="${pageContext.request.contextPath}/admin" method="post" enctype="multipart/form-data">
                <input type="hidden" name="action" value="importRooms">
                <input type="file" name="roomsFile" accept=".csv,text/csv" required>
                <button class="secondary" type="submit">Upload rooms.csv</button>
            </form>
        </div>
    </section>

    <section class="panel">
        <div class="section-head">
            <h2>Seat Allocation</h2>
            <span class="muted">Preview first, then save to the database</span>
        </div>
        <div class="actions">
            <form action="${pageContext.request.contextPath}/admin" method="post">
                <input type="hidden" name="action" value="previewAllocation">
                <input type="hidden" name="examId" value="<%= selectedExamId %>">
                <button class="primary" type="submit">Preview before save</button>
            </form>
            <form action="${pageContext.request.contextPath}/admin" method="post">
                <input type="hidden" name="action" value="saveAllocation">
                <input type="hidden" name="examId" value="<%= selectedExamId %>">
                <button class="success" type="submit">Run allocation</button>
            </form>
            <form action="${pageContext.request.contextPath}/admin" method="post" onsubmit="return confirm('Clear allocations for the selected exam?');">
                <input type="hidden" name="action" value="clearAllocations">
                <input type="hidden" name="examId" value="<%= selectedExamId %>">
                <button class="danger" type="submit">Clear allocations</button>
            </form>
            <form action="${pageContext.request.contextPath}/admin" method="post">
                <input type="hidden" name="action" value="sendEmails">
                <input type="hidden" name="examId" value="<%= selectedExamId %>">
                <button class="secondary" type="submit">Notify all students</button>
            </form>
        </div>
    </section>

    <section class="panel">
        <div class="section-head">
            <h2>Room Capacity Usage</h2>
            <span class="muted"><%= roomUsage == null ? 0 : roomUsage.size() %> rooms loaded</span>
        </div>
        <div class="room-list">
            <% if (roomUsage != null) {
                for (AdminServlet.RoomUsage room : roomUsage) {
                    int percent = room.getCapacity() == 0 ? 0 : Math.min(100, (room.getUsedSeats() * 100) / room.getCapacity());
            %>
            <div class="usage">
                <strong><span><%= room.getRoomId() %></span><span><%= room.getUsedSeats() %>/<%= room.getCapacity() %></span></strong>
                <div class="bar-wrap"><div class="bar" style="width:<%= percent %>%"></div></div>
            </div>
            <% }} %>
        </div>
    </section>

    <% if (previewRows != null) { %>
    <section class="panel">
        <div class="section-head">
            <h2>Allocation Preview</h2>
            <span class="muted"><%= previewRows.size() %> rows</span>
        </div>
        <div class="table-wrap">
        <table>
            <thead><tr><th>USN</th><th>Name</th><th>Department</th><th>Room</th><th>Bench</th><th>Seat</th></tr></thead>
            <tbody>
            <% for (AdminServlet.AllocationPreviewRow row : previewRows) { %>
            <tr>
                <td><%= row.getUsn() %></td>
                <td><%= row.getName() %></td>
                <td><%= row.getDepartment() %></td>
                <td><%= row.getRoomId() %></td>
                <td><%= row.getBenchNumber() %></td>
                <td><%= row.getSeatPosition() %></td>
            </tr>
            <% } %>
            </tbody>
        </table>
        </div>
    </section>
    <% } %>

    <section class="panel" id="progressContainer">
        <div class="section-head">
            <h2>Email Progress</h2>
            <span class="muted">Live status</span>
        </div>
        <div id="studentTicker" class="muted">Waiting for email broadcast activity...</div>
        <br>
        <div class="progress-grid">
            <div class="column"><strong>Waiting <span id="countPending">0</span></strong><div id="listPending"></div></div>
            <div class="column"><strong>Sent <span id="countSuccess">0</span></strong><div id="listSuccess"></div></div>
            <div class="column"><strong>Failed / Skipped <span id="countFailed">0</span></strong><div id="listFailed"></div></div>
        </div>
    </section>
</main>

<script>
    var contextPath = "${pageContext.request.contextPath}";
    var wsUrl = (window.location.protocol === "https:" ? "wss://" : "ws://") + window.location.host + contextPath + "/progressTrack";
    var webSocket = new WebSocket(wsUrl);
    var totalPending = 0, totalSuccess = 0, totalFailed = 0;

    webSocket.onmessage = function(event) {
        var parts = event.data.split("|");
        var statusAction = parts[0];
        if (statusAction === "COMPLETE") { setTicker("Email broadcast completed."); return; }
        if (statusAction === "ERROR") { setTicker("Error: " + safeText(parts[1])); return; }
        var usn = parts[1] || "", name = parts[2] || "", email = parts[3] || "";
        if (statusAction === "PENDING") {
            totalPending++; setCount("countPending", totalPending);
            setTicker("Queued " + name + " for email delivery.");
            appendBadge("listPending", "badge_" + cleanId(usn), usn, name, email, "#f59e0b", "");
        }
        if (statusAction === "SUCCESS") {
            removeBadge(usn); totalPending = Math.max(0, totalPending - 1); totalSuccess++;
            setCount("countPending", totalPending); setCount("countSuccess", totalSuccess);
            setTicker("Sent email to " + name + ".");
            appendBadge("listSuccess", "", usn, name, email, "#22c55e", "");
        }
        if (statusAction === "FAILED") {
            removeBadge(usn); totalPending = Math.max(0, totalPending - 1); totalFailed++;
            setCount("countPending", totalPending); setCount("countFailed", totalFailed);
            setTicker("Skipped or failed delivery for " + name + ".");
            appendBadge("listFailed", "", usn, name, email, "#ef4444", parts[4] || "Unknown error");
        }
    };
    webSocket.onerror = function() { setTicker("Unable to connect to progress updates. Check Tomcat WebSocket support."); };
    function setTicker(text) { document.getElementById("studentTicker").textContent = text; }
    function setCount(id, value) { document.getElementById(id).textContent = value; }
    function appendBadge(targetId, id, usn, name, email, color, detail) {
        var badge = document.createElement("div");
        badge.className = "student-badge"; if (id) badge.id = id; badge.style.borderLeftColor = color;
        badge.innerHTML = "<strong>" + safeText(usn) + "</strong><br>" + safeText(name)
            + "<br><span class=\"muted\">" + safeText(email) + "</span>"
            + (detail ? "<br><span class=\"muted\">" + safeText(detail) + "</span>" : "");
        document.getElementById(targetId).appendChild(badge);
    }
    function removeBadge(usn) { var existing = document.getElementById("badge_" + cleanId(usn)); if (existing) existing.remove(); }
    function cleanId(value) { return String(value).replace(/[^a-zA-Z0-9_-]/g, "_"); }
    function safeText(value) {
        return String(value || "").replace(/[&<>"']/g, function(match) {
            return {"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"}[match];
        });
    }
</script>
</body>
</html>
