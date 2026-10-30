package mx.edu.utch.dgs.gateway.inventory.repository;

import mx.edu.utch.dgs.gateway.inventory.domain.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<StockItem, Long> {

    Optional<StockItem> findByProductId(String productId);
}
