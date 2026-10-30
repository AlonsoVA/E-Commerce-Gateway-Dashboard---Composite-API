package mx.edu.utch.dgs.gateway.composite.client;

import mx.edu.utch.dgs.gateway.catalog.api.ProductResponse;
import mx.edu.utch.dgs.gateway.common.ResourceNotFoundException;
import mx.edu.utch.dgs.gateway.common.UpstreamException;
import mx.edu.utch.dgs.gateway.config.InternalBaseUrl;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CatalogHttpClient {

    private final RestClient restClient;
    private final InternalBaseUrl internalBaseUrl;

    public CatalogHttpClient(RestClient restClient, InternalBaseUrl internalBaseUrl) {
        this.restClient = restClient;
        this.internalBaseUrl = internalBaseUrl;
    }

    public ProductResponse getById(String id) {
        try {
            ProductResponse body = restClient.get()
                    .uri(internalBaseUrl.get() + "/microservices/products/{id}", id)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResourceNotFoundException("catalog", "No existe el producto " + id);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new UpstreamException("El catálogo respondió " + response.getStatusCode().value());
                    })
                    .body(ProductResponse.class);
            if (body == null) {
                throw new UpstreamException("El catálogo no devolvió el producto " + id);
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
        return new UpstreamException("No fue posible consultar el catálogo de productos", exception);
    }
}
