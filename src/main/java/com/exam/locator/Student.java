package com.exam.locator;

public class Student {
    private String usn;
    private String name;
    private String email;
    private String department;
    private int semester;
    private String allocatedRoom; // For tracking assigned room
    private int seatNumber;       // For tracking assigned seat
    private int seatPosition;
    private String examSubject;
    private String examDate;
    private String examTime;

    // Updated 3-argument constructor matching your CSV input layer
    public Student(String usn, String name, String email) {
        this.usn = usn;
        this.name = name;
        this.email = email;
        this.department = "";
        this.semester = 0;
        this.allocatedRoom = "Not Allocated"; // Default state
        this.seatNumber = 0;
        this.seatPosition = 0;
    }

    // Standard Getters and Setters
    public String getUsn() { return usn; }
    public void setUsn(String usn) { this.usn = usn; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getAllocatedRoom() { return allocatedRoom; }
    public void setAllocatedRoom(String allocatedRoom) { this.allocatedRoom = allocatedRoom; }

    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }

    public int getSeatPosition() { return seatPosition; }
    public void setSeatPosition(int seatPosition) { this.seatPosition = seatPosition; }

    public String getExamSubject() { return examSubject; }
    public void setExamSubject(String examSubject) { this.examSubject = examSubject; }

    public String getExamDate() { return examDate; }
    public void setExamDate(String examDate) { this.examDate = examDate; }

    public String getExamTime() { return examTime; }
    public void setExamTime(String examTime) { this.examTime = examTime; }

}
