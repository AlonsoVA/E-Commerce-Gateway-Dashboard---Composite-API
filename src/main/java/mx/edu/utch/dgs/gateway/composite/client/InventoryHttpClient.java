package mx.edu.utch.dgs.gateway.composite.client;

import mx.edu.utch.dgs.gateway.common.ResourceNotFoundException;
import mx.edu.utch.dgs.gateway.common.UpstreamException;
import mx.edu.utch.dgs.gateway.config.InternalBaseUrl;
import mx.edu.utch.dgs.gateway.inventory.api.InventoryResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InventoryHttpClient {

    private final RestClient restClient;
    private final InternalBaseUrl internalBaseUrl;

    public InventoryHttpClient(RestClient restClient, InternalBaseUrl internalBaseUrl) {
        this.restClient = restClient;
        this.internalBaseUrl = internalBaseUrl;
    }

    public InventoryResponse getByProductId(String productId) {
        try {
            InventoryResponse body = restClient.get()
                    .uri(internalBaseUrl.get() + "/microservices/inventory/{productId}", productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResourceNotFoundException("inventory", "No hay registro de inventario para el producto " + productId);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new UpstreamException("El inventario respondió " + response.getStatusCode().value());
                    })
                    .body(InventoryResponse.class);
            if (body == null) {
                throw new UpstreamException("El inventario no devolvió datos de " + productId);
            }
            return body;
        } catch (ResourceNotFoundException | UpstreamException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw unwrap(exception);
        }
    }

    private RuntimeException unwrap(RestClientException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ResourceNotFoundException notFound) {
                return notFound;
            }
            if (current instanceof UpstreamException upstream) {
                return upstream;
            }
            current = current.getCause();
        }
        return new UpstreamException("No fue posible consultar el inventario", exception);
    }
}
