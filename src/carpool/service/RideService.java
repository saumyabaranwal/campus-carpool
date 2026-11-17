package carpool.service;

import carpool.exception.NoSeatsAvailableException;
import carpool.model.*;
import java.util.ArrayList;
import java.util.List;

public class RideService {
    private static final long MINUTE = 60 * 1000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    private ArrayList<User> users = new ArrayList<>();
    private ArrayList<Ride> rides = new ArrayList<>();
    private ArrayList<Booking> bookings = new ArrayList<>();
    private int nextId = 1;

    public RideService() {
        SampleData.fill(this);
    }

    public User register(String id, String name, String contact, String role) {
        if (isBlank(id) || isBlank(name) || isBlank(contact)) {
            throw new IllegalArgumentException("Please fill in all the details");
        }
        id = id.trim();
        name = name.trim();
        contact = contact.trim();
        if (id.length() != 10 || !allDigits(id)) {
            throw new IllegalArgumentException("Student ID must be 10 digits");
        }
        if (!validName(name)) {
            throw new IllegalArgumentException("Name should only have letters");
        }
        if (contact.length() != 10 || !allDigits(contact) || contact.charAt(0) < '6') {
            throw new IllegalArgumentException("Enter a valid 10 digit mobile number");
        }
        for (User u : users) {
            if (u.getId().equalsIgnoreCase(id)) {
                if (!u.getRole().equals(role)) {
                    throw new IllegalArgumentException("This ID is registered as a " + u.getRole());
                }
                u.setName(name);
                u.setContact(contact);
                return u;
            }
        }
        User user;
        if (role.equals("driver")) user = new Driver(id, name, contact);
        else if (role.equals("passenger")) user = new Passenger(id, name, contact);
        else throw new IllegalArgumentException("Choose driver or passenger");
        users.add(user);
        return user;
    }

    public Ride createRide(String driverId, String source, String destination, long time,
                           String type, String number, String model, int seats, double fare) {
        User user = findUser(driverId);
        if (!(user instanceof Driver)) {
            throw new IllegalArgumentException("Only drivers can offer rides");
        }
        Driver driver = (Driver) user;
        if (isBlank(source) || isBlank(destination) || isBlank(number) || isBlank(model)) {
            throw new IllegalArgumentException("Please fill in all the ride details");
        }
        if (source.trim().equalsIgnoreCase(destination.trim())) {
            throw new IllegalArgumentException("Pickup and drop must be different places");
        }
        if (fare < 10 || fare > 5000) {
            throw new IllegalArgumentException("Total cost must be between 10 and 5000 rupees");
        }
        number = number.replace(" ", "").replace("-", "").toUpperCase();
        if (!validPlate(number)) {
            throw new IllegalArgumentException("Enter a valid number plate, like UP16AB1234");
        }
        long now = System.currentTimeMillis();
        if (time < now + 30 * MINUTE) {
            throw new IllegalArgumentException("Departure must be at least 30 minutes from now");
        }
        if (time > now + 30 * DAY) {
            throw new IllegalArgumentException("Rides can only be posted up to 30 days ahead");
        }
        if (clashes(ridesOf(driverId), time)) {
            throw new IllegalArgumentException("You already have a ride around this time");
        }
        Vehicle vehicle;
        if (type.equals("bike")) {
            vehicle = new Bike(number, model.trim());
        } else {
            if (seats < 1 || seats > 6) throw new IllegalArgumentException("Seats must be between 1 and 6");
            vehicle = new Car(number, model.trim(), seats);
        }
        Ride ride = new Ride(nextId++, driver, source.trim(), destination.trim(), time, vehicle, fare);
        rides.add(ride);
        return ride;
    }

    public List<Ride> search(String destination) {
        String query = "";
        if (destination != null) query = destination.trim().toLowerCase();
        List<Ride> found = new ArrayList<>();
        for (Ride r : rides) {
            boolean upcoming = r.getTime() > System.currentTimeMillis();
            boolean matches = r.getDestination().toLowerCase().contains(query);
            if (!r.isCompleted() && upcoming && matches) found.add(r);
        }
        return found;
    }

    public Booking joinRide(int rideId, String passengerId) throws NoSeatsAvailableException {
        User user = findUser(passengerId);
        if (!(user instanceof Passenger)) {
            throw new IllegalArgumentException("Only passengers can join rides");
        }
        Passenger passenger = (Passenger) user;
        Ride ride = findRide(rideId);
        for (Booking b : bookings) {
            if (b.getRide() == ride && b.getPassenger() == passenger && !b.isCancelled()) {
                throw new IllegalArgumentException("You have already booked this ride");
            }
        }
        if (ride.getTime() <= System.currentTimeMillis()) {
            throw new IllegalArgumentException("This ride has already left");
        }
        List<Ride> booked = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getPassenger() == passenger && !b.isCancelled()) booked.add(b.getRide());
        }
        if (clashes(booked, ride.getTime())) {
            throw new IllegalArgumentException("You already have a ride around this time");
        }
        ride.bookSeat();
        Booking booking = new Booking(nextId++, ride, passenger);
        bookings.add(booking);
        return booking;
    }

    public void cancelBooking(int bookingId, String userId) {
        for (Booking b : bookings) {
            if (b.getId() == bookingId && b.getPassenger().getId().equals(userId)) {
                if (b.isCancelled()) throw new IllegalArgumentException("Booking is already cancelled");
                if (b.getRide().getTime() < System.currentTimeMillis() + 30 * MINUTE) {
                    throw new IllegalArgumentException("Bookings cannot be cancelled within 30 minutes of departure");
                }
                b.cancel();
                return;
            }
        }
        throw new IllegalArgumentException("Booking not found");
    }

    public void completeRide(int rideId, String driverId) {
        Ride ride = findRide(rideId);
        if (!ride.getDriver().getId().equals(driverId)) {
            throw new IllegalArgumentException("This is not your ride");
        }
        ride.complete();
    }

    public List<Ride> ridesOf(String driverId) {
        List<Ride> mine = new ArrayList<>();
        for (Ride r : rides) if (r.getDriver().getId().equals(driverId)) mine.add(r);
        return mine;
    }

    public List<Booking> bookingsOf(String passengerId) {
        List<Booking> mine = new ArrayList<>();
        for (Booking b : bookings) if (b.getPassenger().getId().equals(passengerId)) mine.add(b);
        return mine;
    }

    // two trips less than an hour apart are treated as overlapping
    private boolean clashes(List<Ride> existing, long time) {
        for (Ride r : existing) {
            long gap = Math.abs(r.getTime() - time);
            if (!r.isCompleted() && gap < HOUR) return true;
        }
        return false;
    }

    private User findUser(String id) {
        for (User u : users) if (u.getId().equalsIgnoreCase(id)) return u;
        return null;
    }

    private Ride findRide(int id) {
        for (Ride r : rides) if (r.getId() == id) return r;
        throw new IllegalArgumentException("Ride not found");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

    private boolean allDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private boolean validName(String name) {
        if (name.length() < 2 || name.length() > 40) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetter(c) && c != ' ' && c != '.') return false;
        }
        return true;
    }

    // two letters first, four digits last, only letters and digits in between
    private boolean validPlate(String plate) {
        if (plate.length() < 6 || plate.length() > 10) return false;
        for (int i = 0; i < plate.length(); i++) {
            char c = plate.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
            if (!ok) return false;
        }
        boolean lettersFirst = Character.isLetter(plate.charAt(0)) && Character.isLetter(plate.charAt(1));
        return lettersFirst && allDigits(plate.substring(plate.length() - 4));
    }
}
