package mx.edu.utch.dgs.gateway.composite.service;

import mx.edu.utch.dgs.gateway.composite.dto.CallTrace;

final class CallOutcome<T> {

    private final String service;
    private final T body;
    private final Throwable error;
    private final long elapsedMs;

    private CallOutcome(String service, T body, Throwable error, long elapsedMs) {
        this.service = service;
        this.body = body;
        this.error = error;
        this.elapsedMs = elapsedMs;
    }

    static <T> CallOutcome<T> success(String service, T body, long elapsedMs) {
        return new CallOutcome<>(service, body, null, elapsedMs);
    }

    static <T> CallOutcome<T> failure(String service, Throwable error, long elapsedMs) {
        return new CallOutcome<>(service, null, error, elapsedMs);
    }

    boolean ok() {
        return error == null && body != null;
    }

    T body() {
        return body;
    }

    Throwable error() {
        return error;
    }

    CallTrace trace() {
        return new CallTrace(service, ok(), elapsedMs);
    }
}
