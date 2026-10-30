package mx.edu.utch.dgs.gateway.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import mx.edu.utch.dgs.gateway.currency.CurrencyUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> status(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatusCode code = exception.getStatusCode();
        String phrase = code instanceof HttpStatus http ? http.getReasonPhrase() : "Error";
        String message = exception.getReason() == null ? phrase : exception.getReason();
        if (code.is5xxServerError()) {
            log.error("Error HTTP {} en {}", code.value(), request.getRequestURI(), exception);
        }
        return json(code, phrase, message, request);
    }

    @ExceptionHandler(CurrencyUnavailableException.class)
    ResponseEntity<ApiErrorResponse> currency(CurrencyUnavailableException exception, HttpServletRequest request) {
        log.warn("Divisas no disponibles: {}", exception.getMessage());
        return json(HttpStatus.BAD_GATEWAY, HttpStatus.BAD_GATEWAY.getReasonPhrase(), exception.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> constraint(ConstraintViolationException exception, HttpServletRequest request) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .distinct()
                .collect(Collectors.joining(" "));
        return json(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), message, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorResponse> methodValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
        String message = exception.getAllErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? ProductIds.MESSAGE : error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(" "));
        return json(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), message, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), exception);
        return json(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocurrió un error inesperado",
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> json(HttpStatusCode status, String error, String message, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
