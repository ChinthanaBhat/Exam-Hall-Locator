<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Exam Hall Locator</title>
    <style>
        body { font-family: 'Segoe UI', sans-serif; background-color: #E6E6FA; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
        .glass-card { background: rgba(255, 255, 255, 0.4); padding: 40px; border-radius: 15px; box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37); backdrop-filter: blur(8px); text-align: center; }
        input[type="text"] { padding: 10px; width: 250px; border: 1px solid #9966CC; border-radius: 5px; margin-bottom: 20px; }
        button { padding: 10px 20px; background-color: #9966CC; color: white; border: none; border-radius: 5px; cursor: pointer; font-weight: bold; }
        button:hover { background-color: #8A2BE2; }
    </style>
</head>
<body>
<div class="glass-card">
    <h2>Exam Hall Locator</h2>
    <form action="allocate" method="post">
        <input type="text" name="usn" placeholder="Enter USN (e.g. 1BI25MC019)" required>
        <br>
        <button type="submit">Find My Hall</button>
    </form>
</div>
</body>
</html>