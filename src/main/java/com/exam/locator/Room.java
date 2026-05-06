package com.exam.locator;

public class Room {
    private String roomNumber;
    private int capacity;
    private int currentOccupancy;
    private String floor; // Added to store floor data

    public Room(String roomNumber, int capacity, String floor) {
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.floor = floor;
        this.currentOccupancy = 0;
    }

    public String getRoomNumber() { return roomNumber; }
    public String getFloor() { return floor; }
    public int getCapacity() { return capacity; }
    public int getCurrentOccupancy() { return currentOccupancy; }

    public boolean hasSpace() { return currentOccupancy < capacity; }
    public void addStudent() { currentOccupancy++; }
}