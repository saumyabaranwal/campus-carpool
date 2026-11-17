package carpool.model;

public class Driver extends User {
    public Driver(String id, String name, String contact) {
        super(id, name, contact);
    }

    public String getRole() { return "driver"; }
}
