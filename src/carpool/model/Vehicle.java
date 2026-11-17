package carpool.model;

import carpool.fare.FareCalculator;
import java.io.Serializable;

public abstract class Vehicle implements Serializable {
    private final String number;
    private final String model;
    private final int capacity;

    protected Vehicle(String number, String model, int capacity) {
        this.number = number;
        this.model = model;
        this.capacity = capacity;
    }

    public String getNumber() { return number; }
    public String getModel() { return model; }
    public int getCapacity() { return capacity; }

    public abstract String getType();
    public abstract FareCalculator getFareCalculator();
}
