package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Topping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToppingRepository extends JpaRepository<Topping, String> {
    List<Topping> findByBrandIdAndStatus(String brandId, String status);
}
