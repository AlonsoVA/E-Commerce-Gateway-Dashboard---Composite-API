package mx.edu.utch.dgs.gateway.common;

public final class ProductIds {

    public static final String PATTERN = "^[A-Za-z0-9-]{1,16}$";
    public static final String MESSAGE = "El identificador del producto no es válido";

    private ProductIds() {
    }

    public static String normalize(String id) {
        return id.toUpperCase(java.util.Locale.ROOT);
    }
}
