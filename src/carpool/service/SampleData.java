package carpool.service;

import carpool.exception.NoSeatsAvailableException;
import carpool.model.Ride;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// demo rides so the app is not empty the first time it runs
class SampleData {
    static void fill(RideService s) {
        s.register("2501030001", "Aarav Mehta", "9810011001", "driver");
        s.register("2501030002", "Diya Kapoor", "9810011002", "driver");
        s.register("2501030003", "Kabir Singh", "9810011003", "driver");
        s.register("2501030004", "Isha Verma", "9810011004", "passenger");
        s.register("2501030005", "Rohan Gupta", "9810011005", "passenger");

        Ride r1 = s.createRide("2501030001", "JIIT Sector 62", "Noida City Centre", at(1, 9, 0), "car", "UP16AB1234", "Hyundai i20", 3, 160);
        Ride r2 = s.createRide("2501030002", "JIIT Sector 62", "Connaught Place", at(1, 17, 30), "car", "DL3CAF5678", "Maruti Swift", 3, 480);
        Ride r3 = s.createRide("2501030003", "JIIT Sector 62", "Sector 18 Market", at(1, 8, 15), "bike", "UP14K4321", "Honda Shine", 1, 120);
        s.createRide("2501030001", "JIIT Sector 62", "Anand Vihar ISBT", at(2, 18, 0), "car", "UP16AB1234", "Hyundai i20", 4, 300);
        s.createRide("2501030002", "JIIT Sector 62", "Greater Noida", at(2, 10, 0), "car", "DL3CAF5678", "Maruti Swift", 2, 260);
        s.createRide("2501030003", "JIIT Sector 62", "Noida City Centre", at(2, 14, 0), "bike", "UP14K4321", "Honda Shine", 1, 100);

        try {
            s.joinRide(r2.getId(), "2501030004");
            s.joinRide(r3.getId(), "2501030004");
            s.joinRide(r2.getId(), "2501030005");
            s.joinRide(r1.getId(), "2501030005");
        } catch (NoSeatsAvailableException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String at(int daysAhead, int hour, int minute) {
        return LocalDateTime.now().plusDays(daysAhead).truncatedTo(ChronoUnit.DAYS)
                .withHour(hour).withMinute(minute).toString();
    }
}
