package carpool.model;

import carpool.fare.BikeFare;
import carpool.fare.FareCalculator;

public class Bike extends Vehicle {
    public Bike(String number, String model) {
        super(number, model, 1);
    }

    public String getType() { return "bike"; }
    public FareCalculator getFareCalculator() { return new BikeFare(); }
}
