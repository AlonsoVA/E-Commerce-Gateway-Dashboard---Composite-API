package mx.edu.utch.dgs.gateway.catalog.repository;

import mx.edu.utch.dgs.gateway.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findAllByOrderByIdAsc();
}
