<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Seat Allocation</title>
    <style>
        body { font-family: 'Segoe UI', sans-serif; background-color: #E6E6FA; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
        .glass-card { background: rgba(255, 255, 255, 0.4); padding: 40px; border-radius: 15px; box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37); backdrop-filter: blur(8px); text-align: center; min-width: 300px; }
        .highlight { color: #8A2BE2; font-size: 24px; font-weight: bold; margin: 15px 0; }
        a { display: inline-block; margin-top: 20px; padding: 10px 20px; background-color: #9966CC; color: white; text-decoration: none; border-radius: 5px; }
    </style>
</head>
<body>
<div class="glass-card">
    <h2>Allocation Results</h2>
    <% if (request.getAttribute("room") != null) { %>
    <p>Name: <strong>${studentName}</strong></p>
    <p>USN: <strong>${usn}</strong></p>
    <p>Your Exam Hall:</p>
    <div class="highlight">${room}</div>
    <% } else { %>
    <div class="highlight" style="color: red;">${error}</div>
    <% } %>
    <a href="index.jsp">New Search</a>
</div>
</body>
</html>