package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.repository.custom.ProductRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, ProductRepositoryCustom {
    /**
     * Check if a product with the given code exists.
     *
     * @param code the code of the product
     * @return true if the product exists, false otherwise
     */
    boolean existsByCode(String code);

    @Query("""
            SELECT p FROM Product p
            WHERE (:keyword IS NULL OR :keyword = '' OR 
                   LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
                   LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR :categoryId = '' OR p.category.id = :categoryId)
              AND (:status IS NULL OR :status = '' OR p.status = :status)
            """)
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") String categoryId,
            @Param("status") String status,
            Pageable pageable
    );
}
