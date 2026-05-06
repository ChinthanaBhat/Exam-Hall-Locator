package com.exam.locator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@WebServlet("/allocate")
public class AllocationServlet extends HttpServlet {
    private Map<String, Student> studentRegistry = new HashMap<>();

    @Override
    public void init() throws ServletException {
        // Define your two halls with a capacity of 30 each
        Room hall1 = new Room("Hall A-101 (Ground Floor)", 30);
        Room hall2 = new Room("Hall B-202 (First Floor)", 30);

        InputStream is = getServletContext().getResourceAsStream("/WEB-INF/students.csv");
        if (is == null) {
            System.out.println("ERROR: students.csv not found!");
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) { isFirstLine = false; continue; }

                String[] parts = line.split(",");
                // Matches Screenshot 2026-05-06 155945.png: Index 1 is USN, Index 2 is Name
                if (parts.length >= 3) {
                    String usn = parts[1].trim();
                    String name = parts[2].trim();

                    Student student = new Student(usn, name);

                    // Logic: Fill Hall 1 first, then Hall 2
                    if (hall1.hasSpace()) {
                        hall1.addStudent();
                        student.setAssignedRoom(hall1.getRoomNumber());
                    } else if (hall2.hasSpace()) {
                        hall2.addStudent();
                        student.setAssignedRoom(hall2.getRoomNumber());
                    }

                    studentRegistry.put(usn, student);
                }
            }
            System.out.println("--- DATABASE INITIALIZED ---");
            System.out.println("Total Students Loaded: " + studentRegistry.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String searchUsn = request.getParameter("usn");
        if (searchUsn != null) searchUsn = searchUsn.trim();

        Student student = studentRegistry.get(searchUsn);

        if (student != null) {
            request.setAttribute("usn", student.getUsn());
            request.setAttribute("studentName", student.getName());
            request.setAttribute("room", student.getAssignedRoom());
        } else {
            request.setAttribute("error", "USN Not Found");
        }

        request.getRequestDispatcher("/result.jsp").forward(request, response);
    }
}