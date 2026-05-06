package com.exam.locator;

public class Room {
    private String roomNumber;
    private int capacity;
    private int currentOccupancy;

    public Room(String roomNumber, int capacity) {
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.currentOccupancy = 0;
    }

    public String getRoomNumber() { return roomNumber; }
    public boolean hasSpace() { return currentOccupancy < capacity; }
    public void addStudent() { currentOccupancy++; }
}