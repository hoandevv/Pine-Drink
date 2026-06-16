package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.CartItem;
import com.hoandev.pinedrink.entity.dto.request.CartItem.AddCartItemRequest;
import com.hoandev.pinedrink.entity.dto.response.CartItem.CartResponse;

public interface CartService {
    /**
     * Get the active cart for a specific customer and branch.
     *
     * @param customerId the ID of the customer
     * @param branchId the ID of the branch
     * @return the active cart response
     */
    CartResponse getActiveCart(String customerId, String branchId);
    /**
     * Add an item to the cart.
     *
     * @param customerId the ID of the customer
     * @param request the request containing the item details
     * @return the updated cart response
     */
    CartResponse addItemToCart(String customerId, AddCartItemRequest request);


}
