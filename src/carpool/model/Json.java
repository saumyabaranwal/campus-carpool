package carpool.model;

public class Json {
    public static String quote(String s) {
        String safe = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        return "\"" + safe + "\"";
    }

    public static String array(Jsonable[] items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(items[i].toJson());
        }
        sb.append("]");
        return sb.toString();
    }
}
