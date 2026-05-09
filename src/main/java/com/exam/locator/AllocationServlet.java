package com.exam.locator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@WebServlet(name = "AllocationServlet", urlPatterns = {"/allocate", "/publish"})
public class AllocationServlet extends HttpServlet {

    // In-memory storage for fast O(1) lookup
    private Map<String, Student> studentRegistry = new HashMap<>();
    private List<Room> rooms = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        System.out.println("--- System Initialization Started ---");
        loadRooms();
        loadStudentsAndAllocate();
        System.out.println("--- System Ready. Seats Allocated in RAM ---");
    }

    // 1. DATA LOADING LOGIC (Runs once on startup)
    private void loadRooms() {
        rooms.clear();
        InputStream is = getServletContext().getResourceAsStream("/WEB-INF/rooms.csv");
        if (is == null) return;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            br.readLine(); // Skip Header
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 3) {
                    rooms.add(new Room(p[0].trim(), Integer.parseInt(p[1].trim()), p[2].trim()));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadStudentsAndAllocate() {
        studentRegistry.clear();
        InputStream is = getServletContext().getResourceAsStream("/WEB-INF/students.csv");
        if (is == null) return;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            br.readLine(); // Skip Header
            int roomIndex = 0;

            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 4 && roomIndex < rooms.size()) {
                    String usn = p[1].trim();
                    String name = p[2].trim();
                    String email = p[3].trim();

                    Student student = new Student(usn, name, email);
                    Room currentRoom = rooms.get(roomIndex);

                    // If room is full, move to next room
                    if (!currentRoom.hasSpace()) {
                        roomIndex++;
                        if (roomIndex < rooms.size()) {
                            currentRoom = rooms.get(roomIndex);
                        } else { break; }
                    }

                    currentRoom.addStudent();
                    student.setAssignedRoom(currentRoom.getRoomNumber() + " (" + currentRoom.getFloor() + ")");
                    student.setBenchNumber(currentRoom.getCurrentOccupancy());

                    // Store student in HashMap for instant search
                    studentRegistry.put(usn, student);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 2. THE PUBLISH BUTTON LOGIC (Admin Action)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if ("/publish".equals(request.getServletPath())) {
            System.out.println("Publishing notifications...");
            int sentCount = 0;

            for (Student student : studentRegistry.values()) {
                // Call the Email Utility
                EmailUtil.sendEmail(student.getEmail(), student.getName(),
                        student.getAssignedRoom(), student.getBenchNumber());
                sentCount++;
            }

            request.setAttribute("adminMessage", "Success! Sent " + sentCount + " emails.");
            request.getRequestDispatcher("/admin.jsp").forward(request, response);
        }
    }

    // 3. THE SEARCH LOGIC (Student Action)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String searchUsn = request.getParameter("usn");
        Student student = studentRegistry.get(searchUsn != null ? searchUsn.trim() : "");

        if (student != null) {
            request.setAttribute("usn", student.getUsn());
            request.setAttribute("studentName", student.getName());
            request.setAttribute("room", student.getAssignedRoom());
            request.setAttribute("bench", student.getBenchNumber());
        } else {
            request.setAttribute("error", "USN Not Found");
        }
        request.getRequestDispatcher("/result.jsp").forward(request, response);
    }
}