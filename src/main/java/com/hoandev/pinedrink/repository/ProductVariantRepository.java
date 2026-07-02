package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    boolean existsByProductIdAndVariantCode(String productId, String variantCode);

    Page<ProductVariant> findByProductId(String productId, Pageable pageable);

    /**
     * Find product variants by product ID and status, ordered by display order.
     * Use Join Fetch to fetch the associated product entity to avoid N+1 query problem.
     *
     * @param productId the ID of the product
     * @param status    the status of the product variant
     * @return a list of product variants
     */
    @Query("SELECT pv FROM ProductVariant pv JOIN FETCH pv.product p " +
            "WHERE p.id = :productId AND pv.status = :status " +
            "ORDER BY pv.displayOrder ASC")
    List<ProductVariant> findByProductIdAndStatusOrderByDisplayOrder(
            @Param("productId") String productId,
            @Param("status") String status);
}
