package com.exam.locator;

public class Room {
    private String name;      // The room identifier (e.g., "L-101")
    private int capacity;     // Total number of seats available
    private String floor;

    public Room(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
        this.floor = "";
    }

    // Standard Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
}
