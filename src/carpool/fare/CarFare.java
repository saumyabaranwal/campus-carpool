package carpool.fare;

public class CarFare implements FareCalculator {
    // the driver also counts as one of the people sharing the cost
    public int perSeat(double totalFare, int seats) {
        return (int) Math.round(totalFare / (seats + 1));
    }
}
