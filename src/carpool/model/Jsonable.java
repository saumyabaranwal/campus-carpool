package carpool.model;

import java.util.List;
import java.util.stream.Collectors;

public interface Jsonable {
    String toJson();

    static String quote(String s) {
        String safe = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        return "\"" + safe + "\"";
    }

    static String array(List<? extends Jsonable> items) {
        return items.stream().map(Jsonable::toJson).collect(Collectors.joining(",", "[", "]"));
    }
}
