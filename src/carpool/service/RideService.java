package carpool.service;

import carpool.exception.NoSeatsAvailableException;
import carpool.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RideService {
    private static final Path DATA_FILE = Path.of("data", "carpool.dat");

    private ArrayList<User> users = new ArrayList<>();
    private ArrayList<Ride> rides = new ArrayList<>();
    private ArrayList<Booking> bookings = new ArrayList<>();
    private int nextId = 1;

    public RideService() {
        load();
        if (users.isEmpty()) SampleData.fill(this);
    }

    public synchronized User register(String id, String name, String contact, String role) {
        if (isBlank(id) || isBlank(name) || isBlank(contact)) {
            throw new IllegalArgumentException("Please fill in all the details");
        }
        if (!id.trim().matches("\\d{10}")) throw new IllegalArgumentException("Student ID must be 10 digits");
        if (!name.trim().matches("[A-Za-z. ]{2,40}")) throw new IllegalArgumentException("Name should only have letters");
        if (!contact.trim().matches("[6-9]\\d{9}")) throw new IllegalArgumentException("Enter a valid 10 digit mobile number");
        for (User u : users) {
            if (u.getId().equalsIgnoreCase(id)) {
                if (!u.getRole().equals(role)) {
                    throw new IllegalArgumentException("This ID is registered as a " + u.getRole());
                }
                u.setName(name.trim());
                u.setContact(contact.trim());
                save();
                return u;
            }
        }
        User user;
        if (role.equals("driver")) user = new Driver(id.trim(), name.trim(), contact.trim());
        else if (role.equals("passenger")) user = new Passenger(id.trim(), name.trim(), contact.trim());
        else throw new IllegalArgumentException("Choose driver or passenger");
        users.add(user);
        save();
        return user;
    }

    public synchronized Ride createRide(String driverId, String source, String destination, String time,
                                        String type, String number, String model, int seats, double fare) {
        if (!(findUser(driverId) instanceof Driver driver)) {
            throw new IllegalArgumentException("Only drivers can offer rides");
        }
        if (isBlank(source) || isBlank(destination) || isBlank(time) || isBlank(number) || isBlank(model)) {
            throw new IllegalArgumentException("Please fill in all the ride details");
        }
        if (source.trim().equalsIgnoreCase(destination.trim())) {
            throw new IllegalArgumentException("Pickup and drop must be different places");
        }
        if (fare < 10 || fare > 5000) throw new IllegalArgumentException("Total cost must be between 10 and 5000 rupees");
        number = number.replaceAll("[\\s-]", "").toUpperCase();
        if (!number.matches("[A-Z]{2}\\d{1,2}[A-Z]{0,3}\\d{4}")) {
            throw new IllegalArgumentException("Enter a valid number plate, like UP16AB1234");
        }
        LocalDateTime departure = LocalDateTime.parse(time);
        if (departure.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new IllegalArgumentException("Departure must be at least 30 minutes from now");
        }
        if (departure.isAfter(LocalDateTime.now().plusDays(30))) {
            throw new IllegalArgumentException("Rides can only be posted up to 30 days ahead");
        }
        if (clashes(ridesOf(driverId), departure)) {
            throw new IllegalArgumentException("You already have a ride around this time");
        }
        Vehicle vehicle;
        if (type.equals("bike")) {
            vehicle = new Bike(number.trim(), model.trim());
        } else {
            if (seats < 1 || seats > 6) throw new IllegalArgumentException("Seats must be between 1 and 6");
            vehicle = new Car(number.trim(), model.trim(), seats);
        }
        Ride ride = new Ride(nextId++, driver, source.trim(), destination.trim(), time, vehicle, fare);
        rides.add(ride);
        save();
        return ride;
    }

    public synchronized List<Ride> search(String destination) {
        String query = destination == null ? "" : destination.trim().toLowerCase();
        List<Ride> found = new ArrayList<>();
        for (Ride r : rides) {
            if (!r.isCompleted() && r.getTime().isAfter(LocalDateTime.now()) && r.getDestination().toLowerCase().contains(query)) found.add(r);
        }
        return found;
    }

    public synchronized Booking joinRide(int rideId, String passengerId) throws NoSeatsAvailableException {
        if (!(findUser(passengerId) instanceof Passenger passenger)) {
            throw new IllegalArgumentException("Only passengers can join rides");
        }
        Ride ride = findRide(rideId);
        for (Booking b : bookings) {
            if (b.getRide() == ride && b.getPassenger() == passenger && !b.isCancelled()) {
                throw new IllegalArgumentException("You have already booked this ride");
            }
        }
        if (!ride.getTime().isAfter(LocalDateTime.now())) {
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
        save();
        return booking;
    }

    public synchronized void cancelBooking(int bookingId, String userId) {
        for (Booking b : bookings) {
            if (b.getId() == bookingId && b.getPassenger().getId().equals(userId)) {
                if (b.isCancelled()) throw new IllegalArgumentException("Booking is already cancelled");
                if (b.getRide().getTime().isBefore(LocalDateTime.now().plusMinutes(30))) {
                    throw new IllegalArgumentException("Bookings cannot be cancelled within 30 minutes of departure");
                }
                b.cancel();
                save();
                return;
            }
        }
        throw new IllegalArgumentException("Booking not found");
    }

    public synchronized void completeRide(int rideId, String driverId) {
        Ride ride = findRide(rideId);
        if (!ride.getDriver().getId().equals(driverId)) {
            throw new IllegalArgumentException("This is not your ride");
        }
        ride.complete();
        save();
    }

    public synchronized List<Ride> ridesOf(String driverId) {
        List<Ride> mine = new ArrayList<>();
        for (Ride r : rides) if (r.getDriver().getId().equals(driverId)) mine.add(r);
        return mine;
    }

    public synchronized List<Booking> bookingsOf(String passengerId) {
        List<Booking> mine = new ArrayList<>();
        for (Booking b : bookings) if (b.getPassenger().getId().equals(passengerId)) mine.add(b);
        return mine;
    }

    // two trips less than an hour apart are treated as overlapping
    private boolean clashes(List<Ride> existing, LocalDateTime time) {
        for (Ride r : existing) {
            long gap = Math.abs(Duration.between(r.getTime(), time).toMinutes());
            if (!r.isCompleted() && gap < 60) return true;
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
        return s == null || s.isBlank();
    }

    private void save() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(DATA_FILE))) {
                // one write keeps the links between rides, users and bookings intact
                out.writeObject(new Object[]{users, rides, bookings, nextId});
            }
        } catch (IOException e) {
            System.out.println("Could not save data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(DATA_FILE)) return;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(DATA_FILE))) {
            Object[] saved = (Object[]) in.readObject();
            users = (ArrayList<User>) saved[0];
            rides = (ArrayList<Ride>) saved[1];
            bookings = (ArrayList<Booking>) saved[2];
            nextId = (int) saved[3];
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Starting fresh, could not read saved data");
        }
    }
}
