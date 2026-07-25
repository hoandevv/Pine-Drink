package com.hoandev.pinedrink.repository.custom.Impl;

import com.hoandev.pinedrink.entity.dto.report.ProductCatalogReportItemDto;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductSummaryResponse;
import com.hoandev.pinedrink.repository.custom.ProductRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Cài đặt các truy vấn sản phẩm tự viết bằng native SQL.
 * Dữ liệu trả về được map qua SqlResultSetMapping khai báo trong entity Product.
 */
@Repository
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ProductResponse> findProductResponseById(String id) {
        Query query = entityManager.createNativeQuery("""
                SELECT
                    p.id AS id,
                    p.code AS code,
                    p.name AS name,
                    p.description AS description,
                    p.image_url AS imageUrl,
                    p.base_price AS basePrice,
                    p.preparation_minutes AS preparationMinutes,
                    p.is_featured AS featured,
                    p.is_best_seller AS bestSeller,
                    p.available_ice_levels AS availableIceLevels,
                    p.available_sugar_levels AS availableSugarLevels,
                    c.id AS categoryId,
                    c.name AS categoryName,
                    p.status AS status,
                    p.created_at AS createdAt,
                    p.updated_at AS updatedAt
                FROM pr_product p
                JOIN pr_category c ON c.id = p.category_id
                WHERE p.id = :id
                """, "ProductResponseMapping");
        query.setParameter("id", id);
        return ((List<ProductResponse>) query.getResultList()).stream().findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<ProductResponse> searchProductResponses(
            String keyword, String categoryId, String status, String categoryStatus, Pageable pageable) {
        Query query = entityManager.createNativeQuery("""
                SELECT
                    p.id AS id,
                    p.code AS code,
                    p.name AS name,
                    p.description AS description,
                    p.image_url AS imageUrl,
                    p.base_price AS basePrice,
                    p.preparation_minutes AS preparationMinutes,
                    p.is_featured AS featured,
                    p.is_best_seller AS bestSeller,
                    p.available_ice_levels AS availableIceLevels,
                    p.available_sugar_levels AS availableSugarLevels,
                    c.id AS categoryId,
                    c.name AS categoryName,
                    p.status AS status,
                    p.created_at AS createdAt,
                    p.updated_at AS updatedAt
                FROM pr_product p
                JOIN pr_category c ON c.id = p.category_id
                WHERE (:keyword IS NULL OR :keyword = '' OR
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    AND (:categoryId IS NULL OR :categoryId = '' OR p.category_id = :categoryId)
                    AND (:status IS NULL OR :status = '' OR p.status = :status)
                    AND (:categoryStatus IS NULL OR :categoryStatus = '' OR c.status = :categoryStatus)
                """, "ProductResponseMapping");
        setProductSearchParameters(query, keyword, categoryId, status, categoryStatus, true);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        Query countQuery = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM pr_product p
                JOIN pr_category c ON c.id = p.category_id
                WHERE (:keyword IS NULL OR :keyword = '' OR
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    AND (:categoryId IS NULL OR :categoryId = '' OR p.category_id = :categoryId)
                    AND (:status IS NULL OR :status = '' OR p.status = :status)
                    AND (:categoryStatus IS NULL OR :categoryStatus = '' OR c.status = :categoryStatus)
                """);
        setProductSearchParameters(countQuery, keyword, categoryId, status, categoryStatus, true);

        return new PageImpl<>(query.getResultList(), pageable, ((Number) countQuery.getSingleResult()).longValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<ProductSummaryResponse> searchProductSummaryResponses(
            String keyword, String categoryId, String status, Pageable pageable) {
        Query query = entityManager.createNativeQuery("""
                SELECT
                    p.id AS id,
                    p.code AS code,
                    p.name AS name,
                    p.image_url AS imageUrl,
                    p.base_price AS basePrice,
                    p.preparation_minutes AS preparationMinutes,
                    p.is_featured AS featured,
                    p.is_best_seller AS bestSeller,
                    c.id AS categoryId,
                    c.name AS categoryName,
                    p.status AS status
                FROM pr_product p
                JOIN pr_category c ON c.id = p.category_id
                WHERE (:keyword IS NULL OR :keyword = '' OR
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    AND (:categoryId IS NULL OR :categoryId = '' OR p.category_id = :categoryId)
                    AND (:status IS NULL OR :status = '' OR p.status = :status)
                """, "ProductSummaryResponseMapping");
        setProductSearchParameters(query, keyword, categoryId, status, null, false);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        Query countQuery = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM pr_product p
                JOIN pr_category c ON c.id = p.category_id
                WHERE (:keyword IS NULL OR :keyword = '' OR
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    AND (:categoryId IS NULL OR :categoryId = '' OR p.category_id = :categoryId)
                    AND (:status IS NULL OR :status = '' OR p.status = :status)
                """);
        setProductSearchParameters(countQuery, keyword, categoryId, status, null, false);

        return new PageImpl<>(query.getResultList(), pageable, ((Number) countQuery.getSingleResult()).longValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProductCatalogReportItemDto> findProductCatalogReport(
            String status, String categoryId, LocalDateTime fromDate, LocalDateTime toDate) {
        Query query = entityManager.createNativeQuery("""
                SELECT
                    p.code AS productCode,
                    p.name AS productName,
                    c.name AS categoryName,
                    CONCAT(FORMAT(COALESCE(p.base_price, 0), 0), ' VND') AS basePrice,
                    p.status AS status,
                    COALESCE(CAST(p.preparation_minutes AS CHAR), '-') AS preparationMinutes,
                    CASE WHEN p.is_featured = 1 THEN 'YES' ELSE 'NO' END AS featured,
                    CASE WHEN p.is_best_seller = 1 THEN 'YES' ELSE 'NO' END AS bestSeller,
                    COALESCE(GROUP_CONCAT(
                        CASE
                            WHEN pv.id IS NULL THEN NULL
                            ELSE CONCAT(pv.variant_name, ' (+', FORMAT(pv.price_delta, 0), ' VND)')
                        END
                        ORDER BY pv.display_order ASC, pv.variant_name ASC
                        SEPARATOR ', '
                    ), '-') AS variants,
                    COALESCE(DATE_FORMAT(p.created_at, '%d/%m/%Y %H:%i'), '-') AS createdAt
                FROM pr_product p
                JOIN pr_category c ON c.id = p.category_id
                LEFT JOIN pr_product_variant pv ON pv.product_id = p.id
                WHERE (:status IS NULL OR p.status = :status)
                    AND (:categoryId IS NULL OR p.category_id = :categoryId)
                    AND (:fromDate IS NULL OR p.created_at >= :fromDate)
                    AND (:toDate IS NULL OR p.created_at < :toDate)
                GROUP BY p.id, p.code, p.name, c.name, p.base_price, p.status,
                    p.preparation_minutes, p.is_featured, p.is_best_seller, p.created_at
                ORDER BY c.display_order ASC, c.name ASC, p.name ASC
                """, "ProductCatalogReportItemMapping");
        query.setParameter("status", status);
        query.setParameter("categoryId", categoryId);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        return query.getResultList();
    }

    private void setProductSearchParameters(
            Query query, String keyword, String categoryId, String status, String categoryStatus, boolean hasCategoryStatus) {
        query.setParameter("keyword", keyword);
        query.setParameter("categoryId", categoryId);
        query.setParameter("status", status);
        if (hasCategoryStatus) {
            query.setParameter("categoryStatus", categoryStatus);
        }
    }
}
