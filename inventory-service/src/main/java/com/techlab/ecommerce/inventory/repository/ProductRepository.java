package com.techlab.ecommerce.inventory.repository;

import com.techlab.ecommerce.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    /**
     * Atomic stock reservation. The database executes this as one row-level
     * update, so competing consumers cannot both reserve the same units.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE products
               SET stock = stock - :quantity,
                   version = version + 1,
                   updated_at = CURRENT_TIMESTAMP
             WHERE id = :productId
               AND active = TRUE
               AND stock >= :quantity
            """, nativeQuery = true)
    int decrementStockIfAvailable(@Param("productId") Long productId,
                                  @Param("quantity") Integer quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE products
               SET stock = stock + :quantity,
                   version = version + 1,
                   updated_at = CURRENT_TIMESTAMP
             WHERE id = :productId
            """, nativeQuery = true)
    int incrementStock(@Param("productId") Long productId,
                       @Param("quantity") Integer quantity);
}
