package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByBrandIdAndStatus(String brandId, String status);
    List<Product> findByCategoryId(String categoryId);
}
