package carpool.model;

import carpool.fare.CarFare;
import carpool.fare.FareCalculator;

public class Car extends Vehicle {
    public Car(String number, String model, int capacity) {
        super(number, model, capacity);
    }

    public String getType() { return "car"; }
    public FareCalculator getFareCalculator() { return new CarFare(); }
}
