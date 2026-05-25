package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, String> {
    List<Ingredient> findByBrandIdAndStatus(String brandId, String status);
}
