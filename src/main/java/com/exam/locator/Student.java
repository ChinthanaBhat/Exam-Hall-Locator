package com.exam.locator;

public class Student {
    private String usn;
    private String name;
    private String email;
    private String assignedRoom;
    private int benchNumber;

    public Student(String usn, String name,String email) {
        this.usn = usn;
        this.name = name;
        this.email=email;
        this.assignedRoom = "Not Assigned";
        this.benchNumber = 0;
    }

    public String getUsn() { return usn; }
    public String getName() { return name; }
    public String getEmail() {return email;}
    public String getAssignedRoom() { return assignedRoom; }
    public void setAssignedRoom(String assignedRoom) { this.assignedRoom = assignedRoom; }
    public int getBenchNumber() { return benchNumber; }
    public void setBenchNumber(int benchNumber) { this.benchNumber = benchNumber; }
}