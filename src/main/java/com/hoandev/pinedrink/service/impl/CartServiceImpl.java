package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.Cart;
import com.hoandev.pinedrink.entity.CartItem;
import com.hoandev.pinedrink.entity.CartItemTopping;
import com.hoandev.pinedrink.entity.CustomerProfile;
import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.entity.ProductVariant;
import com.hoandev.pinedrink.entity.Topping;
import com.hoandev.pinedrink.entity.dto.request.CartItem.AddCartItemRequest;
import com.hoandev.pinedrink.entity.dto.request.CartItem.CartItemToppingRequest;
import com.hoandev.pinedrink.entity.dto.response.CartItem.CartItemResponse;
import com.hoandev.pinedrink.entity.dto.response.CartItem.CartItemToppingResponse;
import com.hoandev.pinedrink.entity.dto.response.CartItem.CartResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.CartMapper;
import com.hoandev.pinedrink.repository.BranchRepository;
import com.hoandev.pinedrink.repository.BranchProductAvailabilityRepository;
import com.hoandev.pinedrink.repository.CartItemRepository;
import com.hoandev.pinedrink.repository.CartItemToppingRepository;
import com.hoandev.pinedrink.repository.CartRepository;
import com.hoandev.pinedrink.repository.CustomerProfileRepository;
import com.hoandev.pinedrink.repository.ProductRepository;
import com.hoandev.pinedrink.repository.ProductVariantRepository;
import com.hoandev.pinedrink.repository.ToppingRepository;
import com.hoandev.pinedrink.service.BranchVariantDailyStockService;
import com.hoandev.pinedrink.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private static final String ACTIVE = "ACTIVE";
    private static final String NORMAL = "NORMAL";

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartItemToppingRepository cartItemToppingRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final BranchRepository branchRepository;
    private final BranchProductAvailabilityRepository branchProductAvailabilityRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ToppingRepository toppingRepository;
    private final BranchVariantDailyStockService dailyStockService;
    private final CartMapper cartMapper;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getActiveCart(String customerId, String branchId) {
        return cartRepository.findByCustomerIdAndBranchIdAndStatus(customerId, branchId, ACTIVE)
                .map(this::toCartResponse)
                .orElseGet(() -> cartMapper.toEmptyResponse(customerId, branchId));
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(String customerId, AddCartItemRequest request) {
        CustomerProfile customer = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_001));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_001));

        if (!ACTIVE.equals(product.getStatus())) {
            throw new BaseException(ErrorCode.PRODUCT_002);
        }

        ProductVariant variant = getVariant(request.getVariantId(), product.getId());

        int availableQuantity = dailyStockService.getAvailableQuantity(branch.getId(), variant.getId(), LocalDate.now());
        if (availableQuantity < request.getQuantity()) {
            throw new BaseException(ErrorCode.DAILY_STOCK_003);
        }

        Cart cart = cartRepository
                .findByCustomerIdAndBranchIdAndStatus(customerId, request.getBranchId(), ACTIVE)
                .orElseGet(() -> createCart(customer, branch));

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setVariant(variant);
        item.setQuantity(request.getQuantity());
        item.setSugarLevel(defaultValue(request.getSugarLevel(), NORMAL));
        item.setIceLevel(defaultValue(request.getIceLevel(), NORMAL));
        item.setNote(request.getNote());

        BigDecimal unitPrice = product.getBasePrice()
                .add(variant != null ? variant.getPriceDelta() : BigDecimal.ZERO);

        item.setUnitPrice(unitPrice);
        item.setTotalPrice(BigDecimal.ZERO);

        item = cartItemRepository.save(item);

        BigDecimal toppingAmountPerItem = saveToppings(item, request.getToppings());

        BigDecimal totalPrice = unitPrice
                .add(toppingAmountPerItem)
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        item.setTotalPrice(totalPrice);
        cartItemRepository.save(item);

        log.info("Cart item added: cartId={}, itemId={}, customerId={}",
                cart.getId(), item.getId(), customerId);

        return toCartResponse(cart);
    }

    private Cart createCart(CustomerProfile customer, Branch branch) {
        Cart cart = new Cart();
        cart.setCustomer(customer);
        cart.setBranch(branch);
        cart.setStatus(ACTIVE);
        return cartRepository.save(cart);
    }

    private ProductVariant getVariant(String variantId, String productId) {
        if (variantId == null || variantId.isBlank()) {
            return null;
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_007));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new BaseException(ErrorCode.PRODUCT_010);
        }
        return variant;
    }

    private BigDecimal saveToppings(CartItem item, List<CartItemToppingRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal toppingAmount = BigDecimal.ZERO;
        for (CartItemToppingRequest request : requests) {
            Topping topping = toppingRepository.findById(request.getToppingId())
                    .orElseThrow(() -> new BaseException(ErrorCode.TOPPING_001));

            CartItemTopping itemTopping = new CartItemTopping();
            itemTopping.setCartItem(item);
            itemTopping.setTopping(topping);
            itemTopping.setQuantity(request.getQuantity());
            itemTopping.setUnitPrice(topping.getPrice());
            itemTopping.setTotalPrice(topping.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
            cartItemToppingRepository.save(itemTopping);

            toppingAmount = toppingAmount.add(itemTopping.getTotalPrice());
        }
        return toppingAmount;
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository.findByCartId(cart.getId()).stream()
                .map(this::toItemResponse)
                .toList();
        return cartMapper.toResponse(cart, items);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        List<CartItemToppingResponse> toppings = cartItemToppingRepository.findByCartItemId(item.getId()).stream()
                .map(cartMapper::toToppingResponse)
                .toList();
        return cartMapper.toItemResponse(item, toppings);
    }

    private String defaultValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
