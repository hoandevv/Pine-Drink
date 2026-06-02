package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ProductTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductToppingRepository extends JpaRepository<ProductTopping, String> {
    boolean existsByProductIdAndToppingId(String productId, String toppingId);

    List<ProductTopping> findByProductId(String productId);

    List<ProductTopping> findByProductIdAndStatus(String productId, String status);
}
