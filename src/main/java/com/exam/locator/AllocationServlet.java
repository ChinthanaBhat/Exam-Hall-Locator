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
    private List<Room> rooms = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        loadRooms();
        loadStudentsAndAllocate();
    }

    private void loadRooms() {
        InputStream is = getServletContext().getResourceAsStream("/WEB-INF/rooms.csv");
        if (is == null) return;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) { isFirstLine = false; continue; }
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String roomNo = parts[0].trim();
                    int cap = Integer.parseInt(parts[1].trim());
                    String floor = parts[2].trim();
                    rooms.add(new Room(roomNo, cap, floor));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadStudentsAndAllocate() {
        InputStream is = getServletContext().getResourceAsStream("/WEB-INF/students.csv");
        if (is == null) return;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean isFirstLine = true;
            int roomIndex = 0;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) { isFirstLine = false; continue; }
                String[] parts = line.split(",");
                // Matches Index 1 (USN) and Index 2 (Name) from your CSV
                if (parts.length >= 3 && roomIndex < rooms.size()) {
                    String usn = parts[1].trim();
                    String name = parts[2].trim();
                    Student student = new Student(usn, name);

                    Room currentRoom = rooms.get(roomIndex);

                    // If room is full, move to next room
                    if (!currentRoom.hasSpace()) {
                        roomIndex++;
                        if (roomIndex < rooms.size()) {
                            currentRoom = rooms.get(roomIndex);
                        } else {
                            break; // No more room capacity
                        }
                    }

                    currentRoom.addStudent();
                    student.setAssignedRoom(currentRoom.getRoomNumber() + " (" + currentRoom.getFloor() + ")");
                    student.setBenchNumber(currentRoom.getCurrentOccupancy());
                    studentRegistry.put(usn, student);
                }
            }
            System.out.println("Total Students Allocated: " + studentRegistry.size());
        } catch (Exception e) { e.printStackTrace(); }
    }

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