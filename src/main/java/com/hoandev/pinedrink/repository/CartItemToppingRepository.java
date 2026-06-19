package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.CartItemTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemToppingRepository extends JpaRepository<CartItemTopping, String> {
    List<CartItemTopping> findByCartItemId(String cartItemId);
    
    @Query("SELECT t FROM CartItemTopping t WHERE t.cartItem.id IN :cartItemIds")
    List<CartItemTopping> findByCartItemIdIn(@Param("cartItemIds") List<String> cartItemIds);

    void deleteByCartItemId(String cartItemId);
}
