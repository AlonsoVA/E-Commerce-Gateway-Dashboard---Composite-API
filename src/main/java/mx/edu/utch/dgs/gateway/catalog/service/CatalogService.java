package mx.edu.utch.dgs.gateway.catalog.service;

import mx.edu.utch.dgs.gateway.catalog.api.ProductResponse;
import mx.edu.utch.dgs.gateway.catalog.domain.Product;
import mx.edu.utch.dgs.gateway.catalog.repository.ProductRepository;
import mx.edu.utch.dgs.gateway.common.ProductIds;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CatalogService {

    private final ProductRepository products;

    public CatalogService(ProductRepository products) {
        this.products = products;
    }

    public List<ProductResponse> list() {
        return products.findAllByOrderByIdAsc().stream().map(CatalogService::toResponse).toList();
    }

    public ProductResponse getById(String id) {
        String productId = ProductIds.normalize(id);
        return products.findById(productId)
                .map(CatalogService::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el producto " + productId));
    }

    private static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getCategory(),
                product.getPriceUsd()
        );
    }
}
