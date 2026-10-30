package mx.edu.utch.dgs.gateway.config;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class InternalBaseUrl {

    private final AppProperties properties;
    private volatile int port = 8080;

    public InternalBaseUrl(AppProperties properties) {
        this.properties = properties;
    }

    @EventListener
    public void onWebServerReady(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
    }

    public String get() {
        String configured = properties.internalBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return stripTrailingSlash(configured);
        }
        return "http://127.0.0.1:" + port;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
