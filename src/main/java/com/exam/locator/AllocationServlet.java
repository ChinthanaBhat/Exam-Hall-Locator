package com.exam.locator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@WebServlet(name = "AllocationServlet", urlPatterns = {"/allocate"})
public class AllocationServlet extends HttpServlet {
    private Map<String, Student> studentRegistry = new HashMap<>();
    private List<Room> rooms = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        System.out.println("Initializing Allocation System...");
        loadRooms();
        loadStudentsAndAllocate();
    }

    private void loadRooms() {
        rooms.clear();
        InputStream is = getServletContext().getResourceAsStream("/WEB-INF/rooms.csv");
        if (is == null) {
            System.err.println("Error: rooms.csv not found in WEB-INF");
            return;
        }
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
        if (is == null) {
            System.err.println("Error: students.csv not found in WEB-INF");
            return;
        }
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

                    if (!currentRoom.hasSpace()) {
                        roomIndex++;
                        if (roomIndex < rooms.size()) {
                            currentRoom = rooms.get(roomIndex);
                        } else { break; }
                    }

                    currentRoom.addStudent();
                    student.setAssignedRoom(currentRoom.getRoomNumber() + " (" + currentRoom.getFloor() + ")");
                    student.setBenchNumber(currentRoom.getCurrentOccupancy());
                    studentRegistry.put(usn, student);

                    // Send Email
                    EmailUtil.sendEmail(email, name, student.getAssignedRoom(), student.getBenchNumber());
                }
            }
            System.out.println("Allocation Complete. Registry Size: " + studentRegistry.size());
        } catch (Exception e) {
            System.err.println("FATAL ERROR DURING ALLOCATION:");
            e.printStackTrace();
        }
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