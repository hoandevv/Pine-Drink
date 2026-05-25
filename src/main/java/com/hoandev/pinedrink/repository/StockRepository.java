package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {
    Optional<Stock> findByBranchIdAndIngredientId(String branchId, String ingredientId);
    List<Stock> findByBranchId(String branchId);
}
