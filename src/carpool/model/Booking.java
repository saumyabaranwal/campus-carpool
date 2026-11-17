package carpool.model;


public class Booking implements Jsonable {
    private final int id;
    private final Ride ride;
    private final Passenger passenger;
    private final int fare;
    private boolean cancelled;

    public Booking(int id, Ride ride, Passenger passenger) {
        this.id = id;
        this.ride = ride;
        this.passenger = passenger;
        this.fare = ride.farePerSeat();
    }

    public int getId() { return id; }
    public Ride getRide() { return ride; }
    public Passenger getPassenger() { return passenger; }
    public boolean isCancelled() { return cancelled; }

    public void cancel() {
        cancelled = true;
        ride.releaseSeat();
    }

    public String toJson() {
        return "{\"id\":" + id + ",\"fare\":" + fare + ",\"cancelled\":" + cancelled
                + ",\"ride\":" + ride.toJson() + "}";
    }
}
