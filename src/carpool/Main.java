package carpool;

import carpool.exception.NoSeatsAvailableException;
import carpool.model.Json;
import carpool.model.Jsonable;
import carpool.service.RideService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final RideService service = new RideService();
    private static final File WEB = new File("web");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api", ex -> handleApi(ex));
        server.createContext("/", ex -> handleStatic(ex));
        server.start();
        System.out.println("Campus Carpooling running at http://localhost:8080");
    }

    private static void handleApi(HttpExchange ex) throws IOException {
        int status = 200;
        String body;
        try {
            body = route(ex.getRequestURI().getPath(), readParams(ex));
        } catch (NoSeatsAvailableException e) {
            status = 409;
            body = error(e.getMessage());
        } catch (NumberFormatException e) {
            status = 400;
            body = error("Enter valid numbers for seats and fare");
        } catch (IllegalArgumentException e) {
            status = 400;
            body = error(e.getMessage());
        }
        send(ex, status, "application/json", body.getBytes(StandardCharsets.UTF_8));
    }

    private static String route(String path, Map<String, String> p) throws NoSeatsAvailableException {
        switch (path) {
            case "/api/login":
                return service.register(p.get("id"), p.get("name"), p.get("contact"), p.get("role")).toJson();
            case "/api/rides/search":
                return Json.array(service.search(p.get("destination")).toArray(new Jsonable[0]));
            case "/api/rides/create":
                return service.createRide(p.get("userId"), p.get("source"), p.get("destination"), Long.parseLong(p.get("time")),
                        p.get("type"), p.get("number"), p.get("model"),
                        Integer.parseInt(p.getOrDefault("seats", "1")), Double.parseDouble(p.get("fare"))).toJson();
            case "/api/rides/complete":
                service.completeRide(Integer.parseInt(p.get("rideId")), p.get("userId"));
                return "{}";
            case "/api/rides/mine":
                return Json.array(service.ridesOf(p.get("userId")).toArray(new Jsonable[0]));
            case "/api/book":
                return service.joinRide(Integer.parseInt(p.get("rideId")), p.get("userId")).toJson();
            case "/api/cancel":
                service.cancelBooking(Integer.parseInt(p.get("bookingId")), p.get("userId"));
                return "{}";
            case "/api/bookings":
                return Json.array(service.bookingsOf(p.get("userId")).toArray(new Jsonable[0]));
            default:
                throw new IllegalArgumentException("Unknown request");
        }
    }

    private static Map<String, String> readParams(HttpExchange ex) throws IOException {
        String raw = ex.getRequestURI().getRawQuery();
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String all = (raw == null ? "" : raw) + "&" + body;
        Map<String, String> params = new HashMap<>();
        for (String pair : all.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        File file = new File(WEB, path);
        // do not serve anything outside the web folder
        if (!file.getCanonicalPath().startsWith(WEB.getCanonicalPath()) || !file.isFile()) {
            send(ex, 404, "text/plain", "Not found".getBytes());
            return;
        }
        String name = file.getName();
        String type = "text/html";
        if (name.endsWith(".css")) type = "text/css";
        else if (name.endsWith(".js")) type = "text/javascript";
        FileInputStream in = new FileInputStream(file);
        byte[] data = in.readAllBytes();
        in.close();
        send(ex, 200, type + "; charset=utf-8", data);
    }

    private static String error(String message) {
        return "{\"error\":" + Json.quote(message) + "}";
    }

    private static void send(HttpExchange ex, int status, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }
}
