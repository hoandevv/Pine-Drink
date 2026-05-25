package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.CartItemTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemToppingRepository extends JpaRepository<CartItemTopping, String> {
    List<CartItemTopping> findByCartItemId(String cartItemId);
}
