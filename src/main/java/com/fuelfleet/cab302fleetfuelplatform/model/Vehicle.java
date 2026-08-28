package com.fuelfleet.cab302fleetfuelplatform.model;

public class Vehicle {
    private int id;
    private String registration;
    private String make;
    private String model;

    public Vehicle() {}

    public Vehicle(int id, String registration, String make, String model) {
        this.id = id;
        this.registration = registration;
        this.make = make;
        this.model = model;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRegistration() { return registration; }
    public void setRegistration(String registration) { this.registration = registration; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
