package carpool.model;

import carpool.exception.NoSeatsAvailableException;
import java.io.Serializable;
import java.time.LocalDateTime;

public class Ride implements Serializable, Jsonable {
    private final int id;
    private final Driver driver;
    private final String source;
    private final String destination;
    private final String time;
    private final Vehicle vehicle;
    private final double totalFare;
    private int seatsLeft;
    private boolean completed;

    public Ride(int id, Driver driver, String source, String destination,
                String time, Vehicle vehicle, double totalFare) {
        this.id = id;
        this.driver = driver;
        this.source = source;
        this.destination = destination;
        this.time = time;
        this.vehicle = vehicle;
        this.totalFare = totalFare;
        this.seatsLeft = vehicle.getCapacity();
    }

    public int getId() { return id; }
    public Driver getDriver() { return driver; }
    public String getDestination() { return destination; }
    public LocalDateTime getTime() { return LocalDateTime.parse(time); }
    public int getSeatsLeft() { return seatsLeft; }
    public boolean isCompleted() { return completed; }

    public void bookSeat() throws NoSeatsAvailableException {
        if (completed) throw new NoSeatsAvailableException("This ride is already over");
        if (seatsLeft == 0) throw new NoSeatsAvailableException("No seats left on this ride");
        seatsLeft--;
    }

    public void releaseSeat() {
        if (seatsLeft < vehicle.getCapacity()) seatsLeft++;
    }

    public void complete() { completed = true; }

    public int farePerSeat() {
        return vehicle.getFareCalculator().perSeat(totalFare, vehicle.getCapacity());
    }

    public String toJson() {
        return "{\"id\":" + id
                + ",\"driver\":" + Jsonable.quote(driver.getName())
                + ",\"contact\":" + Jsonable.quote(driver.getContact())
                + ",\"source\":" + Jsonable.quote(source)
                + ",\"destination\":" + Jsonable.quote(destination)
                + ",\"time\":" + Jsonable.quote(time)
                + ",\"vehicle\":" + Jsonable.quote(vehicle.getType())
                + ",\"model\":" + Jsonable.quote(vehicle.getModel())
                + ",\"number\":" + Jsonable.quote(vehicle.getNumber())
                + ",\"capacity\":" + vehicle.getCapacity()
                + ",\"seatsLeft\":" + seatsLeft
                + ",\"fare\":" + farePerSeat()
                + ",\"completed\":" + completed + "}";
    }
}
