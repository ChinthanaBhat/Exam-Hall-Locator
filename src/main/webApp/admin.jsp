<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Exam Hall Admin</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f0f2f5; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
        .container { background: white; padding: 2rem; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); text-align: center; width: 400px; }
        h1 { color: #1a1a1a; margin-bottom: 1rem; }
        .btn { background-color: #7209b7; color: white; border: none; padding: 12px 24px; border-radius: 8px; font-size: 1rem; cursor: pointer; transition: 0.3s; font-weight: 600; }
        .btn:hover { background-color: #560bad; transform: translateY(-2px); }
        .status { margin-top: 1.5rem; padding: 10px; border-radius: 6px; background-color: #d1fae5; color: #065f46; font-weight: 500; }
    </style>
</head>
<body>
<div class="container">
    <h1>Admin Console</h1>
    <p>Allocation is complete and stored in RAM.</p>

    <form action="publish" method="get">
        <button type="submit" class="btn">🚀 Notify All Students</button>
    </form>

    <% if(request.getAttribute("adminMessage") != null) { %>
    <div class="status">
        <%= request.getAttribute("adminMessage") %>
    </div>
    <% } %>

    <p style="margin-top: 20px;"><a href="index.jsp" style="color: #7209b7; text-decoration: none;">← Go to Student Search</a></p>
</div>
</body>
</html>