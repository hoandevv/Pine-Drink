package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    List<ProductVariant> findByProductIdAndStatusOrderByDisplayOrder(String productId, String status);
}
