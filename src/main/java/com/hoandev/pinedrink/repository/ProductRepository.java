package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    List<Product> findByStatus(String status);

    List<Product> findByCategoryId(String categoryId);

    Page<Product> findByCategoryId(String categoryId, Pageable pageable);
}
