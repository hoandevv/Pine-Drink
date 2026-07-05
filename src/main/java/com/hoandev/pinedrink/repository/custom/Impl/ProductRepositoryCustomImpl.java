package com.hoandev.pinedrink.repository.custom.Impl;

import com.hoandev.pinedrink.repository.custom.ProductRepositoryCustom;
import com.hoandev.pinedrink.repository.result.ProductCatalogResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Cài đặt các truy vấn sản phẩm tự viết bằng native SQL.
 * Dữ liệu trả về được map qua ProductCatalogResultMapping khai báo trong entity Product.
 */
@Repository
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<ProductCatalogResult> findProductCatalogReport(String status, String categoryId) {
        Query query = entityManager.createNativeQuery("""
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
                """, "ProductCatalogResultMapping");
        query.setParameter("status", status);
        query.setParameter("categoryId", categoryId);
        return query.getResultList();
    }
}
