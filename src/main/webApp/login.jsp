<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Admin Login | Exam Hall Locator</title>
    <style>
        :root {
            --bg: #eef2f6;
            --surface: #ffffff;
            --line: #d8e0ea;
            --text: #172033;
            --muted: #657084;
            --primary: #1d4ed8;
            --danger: #b91c1c;
        }
        * {
            box-sizing: border-box;
        }
        body {
            font-family: "Segoe UI", Arial, sans-serif;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            margin: 0;
            padding: 22px;
            background: var(--bg);
            color: var(--text);
        }
        body:before {
            content: "";
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            height: 5px;
            background: linear-gradient(90deg, #1d4ed8, #0f766e, #b45309);
        }
        .login-card {
            width: 100%;
            max-width: 410px;
            background: var(--surface);
            border: 1px solid var(--line);
            border-radius: 8px;
            box-shadow: 0 16px 34px rgba(23, 32, 51, 0.09);
            padding: 34px;
        }
        h1 {
            font-size: 27px;
            margin: 0 0 8px;
            text-align: center;
        }
        .subtitle {
            margin: 0 0 24px;
            text-align: center;
            color: var(--muted);
            font-size: 14px;
        }
        label {
            display: block;
            margin: 14px 0 6px;
            font-size: 14px;
            font-weight: 750;
            color: #334155;
        }
        input {
            width: 100%;
            padding: 12px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            font-size: 15px;
            color: var(--text);
        }
        input:focus {
            outline: 3px solid rgba(29, 78, 216, 0.16);
            border-color: var(--primary);
        }
        button {
            width: 100%;
            margin-top: 20px;
            padding: 12px;
            border: 0;
            border-radius: 6px;
            background: var(--primary);
            color: #ffffff;
            font-size: 15px;
            font-weight: 750;
            cursor: pointer;
        }
        button:hover {
            background: #1e40af;
        }
        .error {
            margin-bottom: 16px;
            padding: 11px 13px;
            background: #fef2f2;
            border: 1px solid #fecaca;
            border-radius: 8px;
            color: var(--danger);
            font-size: 14px;
            font-weight: 700;
        }
        .student-link {
            display: block;
            margin-top: 18px;
            text-align: center;
            color: var(--primary);
            font-size: 14px;
            text-decoration: none;
            font-weight: 750;
        }
    </style>
</head>
<body>
<main class="login-card">
    <h1>Admin Login</h1>
    <p class="subtitle">Access exam sessions, imports, allocations, and notifications.</p>

    <% if (request.getAttribute("errorMessage") != null) { %>
    <div class="error"><%= request.getAttribute("errorMessage") %></div>
    <% } %>

    <form action="${pageContext.request.contextPath}/login" method="post">
        <label for="username">Username</label>
        <input type="text" id="username" name="username" autocomplete="username" required autofocus>

        <label for="password">Password</label>
        <input type="password" id="password" name="password" autocomplete="current-password" required>

        <button type="submit">Sign In</button>
    </form>

    <a class="student-link" href="${pageContext.request.contextPath}/">Student search</a>
</main>
</body>
</html>
