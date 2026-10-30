package mx.edu.utch.dgs.gateway.common;

public class ResourceNotFoundException extends RuntimeException {

    private final String service;

    public ResourceNotFoundException(String service, String message) {
        super(message);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
