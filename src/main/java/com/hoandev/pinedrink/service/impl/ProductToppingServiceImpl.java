package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.entity.ProductTopping;
import com.hoandev.pinedrink.entity.Topping;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.AssignProductToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductToppingStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductToppingResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductToppingSummaryResponse;
import com.hoandev.pinedrink.entity.enums.ProductStatus;
import com.hoandev.pinedrink.entity.enums.ProductToppingStatus;
import com.hoandev.pinedrink.entity.enums.ToppingStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.ProductToppingMapper;
import com.hoandev.pinedrink.repository.ProductRepository;
import com.hoandev.pinedrink.repository.ProductToppingRepository;
import com.hoandev.pinedrink.repository.ToppingRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.ProductToppingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductToppingServiceImpl implements ProductToppingService {
    private final ProductToppingRepository productToppingRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final ProductToppingMapper productToppingMapper;
    private final AccessScopeService accessScopeService;

    @Override
    @Transactional
    public ProductToppingResponse assign(String productId, AssignProductToppingRequest request) {
        accessScopeService.assertSystemAccess();
        Product product = getProductOrThrow(productId);
        Topping topping = getToppingOrThrow(request.getToppingId());
        if (productToppingRepository.existsByProductIdAndToppingId(productId, request.getToppingId())) {
            throw new BaseException(ErrorCode.TOPPING_005);
        }
        ProductTopping productTopping = productToppingMapper.toEntity(request, product, topping);
        productTopping.setStatus(ProductToppingStatus.ACTIVE.getValue());
        productTopping = productToppingRepository.save(productTopping);
        log.info("Product topping assigned: id={}, productId={}, toppingId={}", productTopping.getId(), productId, request.getToppingId());
        return productToppingMapper.toResponse(productTopping);
    }

    @Override
    @Transactional
    public ProductToppingResponse update(String productId, String productToppingId, UpdateProductToppingRequest request) {
        accessScopeService.assertSystemAccess();
        ProductTopping productTopping = getProductToppingOrThrow(productId, productToppingId);
        productToppingMapper.updateEntity(productTopping, request);
        productTopping = productToppingRepository.save(productTopping);
        log.info("Product topping updated: id={}, productId={}, toppingId={}", productTopping.getId(), productId, productTopping.getTopping().getId());
        return productToppingMapper.toResponse(productTopping);
    }

    @Override
    @Transactional
    public ProductToppingResponse updateStatus(String productId, String productToppingId, UpdateProductToppingStatusRequest request) {
        accessScopeService.assertSystemAccess();
        ProductTopping productTopping = getProductToppingOrThrow(productId, productToppingId);
        productTopping.setStatus(request.getStatus().getValue());
        productTopping = productToppingRepository.save(productTopping);
        log.info("Product topping status updated: id={}, productId={}, toppingId={}, status={}", productTopping.getId(), productId, productTopping.getTopping().getId(), productTopping.getStatus());
        return productToppingMapper.toResponse(productTopping);
    }

    @Override
    @Transactional
    public void delete(String productId, String productToppingId) {
        accessScopeService.assertSystemAccess();
        ProductTopping productTopping = getProductToppingOrThrow(productId, productToppingId);
        if (ProductToppingStatus.INACTIVE.getValue().equals(productTopping.getStatus())) {
            throw new BaseException(ErrorCode.TOPPING_006);
        }
        productTopping.setStatus(ProductToppingStatus.INACTIVE.getValue());
        productToppingRepository.save(productTopping);
        log.info("Product topping deleted (soft): id={}, productId={}, toppingId={}", productTopping.getId(), productId, productTopping.getTopping().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductToppingResponse getById(String productId, String productToppingId) {
        return productToppingMapper.toResponse(getProductToppingOrThrow(productId, productToppingId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductToppingSummaryResponse> getAll(String productId) {
        getProductOrThrow(productId);
        return productToppingRepository.findByProductId(productId)
                .stream()
                .map(productToppingMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductToppingSummaryResponse> getAllActive(String productId) {
        Product product = getProductOrThrow(productId);
        if (!ProductStatus.ACTIVE.getValue().equals(product.getStatus()) && !ProductStatus.OUT_OF_STOCK.getValue().equals(product.getStatus())) {
            return List.of();
        }
        return productToppingRepository.findByProductIdAndStatus(productId, ProductToppingStatus.ACTIVE.getValue())
                .stream()
                .filter(item -> item.getTopping() != null && ToppingStatus.ACTIVE.getValue().equals(item.getTopping().getStatus()))
                .map(productToppingMapper::toSummaryResponse)
                .toList();
    }

    private Product getProductOrThrow(String productId) {
        return productRepository.findById(productId).orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_001));
    }

    private Topping getToppingOrThrow(String toppingId) {
        return toppingRepository.findById(toppingId).orElseThrow(() -> new BaseException(ErrorCode.TOPPING_001));
    }

    private ProductTopping getProductToppingOrThrow(String productId, String productToppingId) {
        ProductTopping productTopping = productToppingRepository.findById(productToppingId)
                .orElseThrow(() -> new BaseException(ErrorCode.TOPPING_004));
        if (productTopping.getProduct() == null || !productId.equals(productTopping.getProduct().getId())) {
            throw new BaseException(ErrorCode.TOPPING_007);
        }
        return productTopping;
    }
}
