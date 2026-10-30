package mx.edu.utch.dgs.gateway.currency;

public class CurrencyUnavailableException extends RuntimeException {

    public CurrencyUnavailableException(String message) {
        super(message);
    }

    public CurrencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
