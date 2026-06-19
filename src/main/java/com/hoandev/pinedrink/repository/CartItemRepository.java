package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {
    List<CartItem> findByCartId(String cartId);

    @Query("""
            SELECT i FROM CartItem i
            WHERE i.cart.id = :cartId
              AND i.product.id = :productId
              AND ((:variantId IS NULL AND i.variant IS NULL) OR i.variant.id = :variantId)
              AND i.sugarLevel = :sugarLevel
              AND i.iceLevel = :iceLevel
              AND COALESCE(i.note, '') = COALESCE(:note, '')
            """)
    List<CartItem> findMatchingItems(
            @Param("cartId") String cartId,
            @Param("productId") String productId,
            @Param("variantId") String variantId,
            @Param("sugarLevel") String sugarLevel,
            @Param("iceLevel") String iceLevel,
            @Param("note") String note);
}
