package mx.edu.utch.dgs.gateway.composite.dto;

public record CallTrace(String service, boolean ok, long elapsedMs) {
}
