package carpool.model;


public abstract class User implements Jsonable {
    private final String id;
    private String name;
    private String contact;

    protected User(String id, String name, String contact) {
        this.id = id;
        this.name = name;
        this.contact = contact;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public void setName(String name) { this.name = name; }
    public void setContact(String contact) { this.contact = contact; }

    public abstract String getRole();

    public String toJson() {
        return "{\"id\":" + Json.quote(id) + ",\"name\":" + Json.quote(name)
                + ",\"contact\":" + Json.quote(contact) + ",\"role\":" + Json.quote(getRole()) + "}";
    }
}
