package carpool.model;

public class Passenger extends User {
    public Passenger(String id, String name, String contact) {
        super(id, name, contact);
    }

    public String getRole() { return "passenger"; }
}
