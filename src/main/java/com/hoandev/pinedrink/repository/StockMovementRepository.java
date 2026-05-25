package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, String> {
    List<StockMovement> findByBranchIdAndIngredientIdOrderByCreatedAtAsc(String branchId, String ingredientId);
}
