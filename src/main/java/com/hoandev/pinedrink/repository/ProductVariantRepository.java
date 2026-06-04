package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    boolean existsByProductIdAndVariantCode(String productId, String variantCode);

    Page<ProductVariant> findByProductId(String productId, Pageable pageable);

    List<ProductVariant> findByProductIdAndStatusOrderByDisplayOrder(String productId, String status);
}
