package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    boolean existsByBrandIdAndCode(String brandId, String code);

    boolean existsByBrandIdAndCodeAndIdNot(String brandId, String code, String id);

    List<Product> findByBrandIdAndStatus(String brandId, String status);

    List<Product> findByCategoryId(String categoryId);

    Page<Product> findByBrandId(String brandId, Pageable pageable);

    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    Page<Product> findByBrandIdAndCategoryId(String brandId, String categoryId, Pageable pageable);
}
