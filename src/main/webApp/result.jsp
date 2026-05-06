<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Seat Allocation</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #E6E6FA; /* Lavender background */
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
        }
        .glass-card {
            background: rgba(255, 255, 255, 0.4); /* Glassmorphism effect */
            padding: 40px;
            border-radius: 15px;
            box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
            backdrop-filter: blur(8px);
            text-align: center;
            min-width: 320px;
        }
        .highlight {
            color: #8A2BE2; /* Amethyst color */
            font-size: 28px;
            font-weight: bold;
            margin: 10px 0;
        }
        .bench-label {
            font-size: 18px;
            color: #4B0082;
            margin-top: 15px;
        }
        a {
            display: inline-block;
            margin-top: 25px;
            padding: 10px 20px;
            background-color: #9966CC;
            color: white;
            text-decoration: none;
            border-radius: 5px;
            font-weight: bold;
        }
        a:hover {
            background-color: #8A2BE2;
        }
    </style>
</head>
<body>

<div class="glass-card">
    <h2>Allocation Details</h2>

    <%-- Display logic for successful search --%>
    <% if (request.getAttribute("room") != null) { %>
    <p>Name: <strong>${studentName}</strong></p>
    <p>USN: <strong>${usn}</strong></p>

    <p>Assigned Hall:</p>
    <div class="highlight">${room}</div>

    <p class="bench-label">Your Bench Number:</p>
    <div class="highlight" style="font-size: 36px;">#${bench}</div>

    <% } else { %>
    <%-- Display logic for "Not Found" error --%>
    <div class="highlight" style="color: #D32F2F;">${error}</div>
    <% } %>

    <a href="index.jsp">Search Another USN</a>
</div>

</body>
</html>