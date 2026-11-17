package carpool.fare;

public class BikeFare implements FareCalculator {
    public int perSeat(double totalFare, int seats) {
        return (int) Math.round(totalFare * 0.6);
    }
}
