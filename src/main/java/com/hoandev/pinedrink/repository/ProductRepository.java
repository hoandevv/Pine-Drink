package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.repository.projection.ProductCatalogProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    List<Product> findByStatus(String status);

    List<Product> findByCategoryId(String categoryId);

    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    @Query(value = """
            SELECT
                p.code AS productCode,
                p.name AS productName,
                c.name AS categoryName,
                p.base_price AS basePrice,
                p.status AS status,
                p.preparation_minutes AS preparationMinutes,
                p.is_featured AS featured,
                p.is_best_seller AS bestSeller,
                GROUP_CONCAT(
                    CASE
                        WHEN pv.id IS NULL THEN NULL
                        ELSE CONCAT(pv.variant_name, ' (+', FORMAT(pv.price_delta, 0), ' VND)')
                    END
                    ORDER BY pv.display_order ASC, pv.variant_name ASC
                    SEPARATOR ', '
                ) AS variants,
                p.created_at AS createdAt
            FROM pr_product p
            JOIN pr_category c ON c.id = p.category_id
            LEFT JOIN pr_product_variant pv ON pv.product_id = p.id
            WHERE (:status IS NULL OR p.status = :status)
                AND (:categoryId IS NULL OR p.category_id = :categoryId)
            GROUP BY p.id, p.code, p.name, c.name, p.base_price, p.status,
                p.preparation_minutes, p.is_featured, p.is_best_seller, p.created_at
            ORDER BY c.display_order ASC, c.name ASC, p.name ASC
            """, nativeQuery = true)
    List<ProductCatalogProjection> findProductCatalogReport(
            @Param("status") String status,
            @Param("categoryId") String categoryId
    );
}
