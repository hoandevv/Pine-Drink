package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.*;
import com.hoandev.pinedrink.entity.dto.request.Branch.*;
import com.hoandev.pinedrink.entity.dto.response.Branch.*;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.BranchAvailabilityMapper;
import com.hoandev.pinedrink.repository.*;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.BranchAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchAvailabilityServiceImpl implements BranchAvailabilityService {
    private final BranchProductAvailabilityRepository branchProductAvailabilityRepository;
    private final BranchToppingAvailabilityRepository branchToppingAvailabilityRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final BranchAvailabilityMapper branchAvailabilityMapper;
    private final AccessScopeService accessScopeService;

    @Override
    @Transactional
    public BranchProductAvailabilityResponse createProductAvailability(String branchId, CreateBranchProductAvailabilityRequest request) {
        accessScopeService.assertCanManageBranch(branchId);
        validateAvailabilityRange(request.getAvailableFrom(), request.getAvailableTo());
        if (branchProductAvailabilityRepository.existsByBranchIdAndProductId(branchId, request.getProductId())) {
            throw new BaseException(ErrorCode.BRANCH_009);
        }
        Branch branch = getBranchOrThrow(branchId);
        Product product = getProductOrThrow(request.getProductId());
        BranchProductAvailability availability = branchAvailabilityMapper.toProductEntity(request, branch, product);
        availability = branchProductAvailabilityRepository.save(availability);
        log.info("Branch product availability created: id={}, branchId={}, productId={}", availability.getId(), branchId, product.getId());
        return branchAvailabilityMapper.toProductResponse(availability);
    }

    @Override
    @Transactional
    public BranchProductAvailabilityResponse updateProductAvailability(String branchId, String id, UpdateBranchProductAvailabilityRequest request) {
        accessScopeService.assertCanManageBranch(branchId);
        BranchProductAvailability availability = getProductAvailabilityOrThrow(branchId, id);
        Product product = null;
        if (request.getProductId() != null) {
            if (branchProductAvailabilityRepository.existsByBranchIdAndProductIdAndIdNot(branchId, request.getProductId(), id)) {
                throw new BaseException(ErrorCode.BRANCH_009);
            }
            product = getProductOrThrow(request.getProductId());
        }
        branchAvailabilityMapper.updateProductEntity(availability, request, product);
        validateAvailabilityRange(availability.getAvailableFrom(), availability.getAvailableTo());
        availability = branchProductAvailabilityRepository.save(availability);
        log.info("Branch product availability updated: id={}, branchId={}", availability.getId(), branchId);
        return branchAvailabilityMapper.toProductResponse(availability);
    }

    @Override
    @Transactional
    public void deleteProductAvailability(String branchId, String id) {
        accessScopeService.assertCanManageBranch(branchId);
        BranchProductAvailability availability = getProductAvailabilityOrThrow(branchId, id);
        branchProductAvailabilityRepository.delete(availability);
        log.info("Branch product availability deleted: id={}, branchId={}", id, branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchProductAvailabilityResponse getProductAvailability(String branchId, String id) {
        accessScopeService.assertCanAccessBranch(branchId);
        return branchAvailabilityMapper.toProductResponse(getProductAvailabilityOrThrow(branchId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchProductAvailabilityResponse> getProductAvailabilities(String branchId) {
        accessScopeService.assertCanAccessBranch(branchId);
        getBranchOrThrow(branchId);
        return branchProductAvailabilityRepository.findByBranchId(branchId).stream()
                .map(branchAvailabilityMapper::toProductResponse)
                .toList();
    }

    @Override
    @Transactional
    public BranchToppingAvailabilityResponse createToppingAvailability(String branchId, CreateBranchToppingAvailabilityRequest request) {
        accessScopeService.assertCanManageBranch(branchId);
        if (branchToppingAvailabilityRepository.existsByBranchIdAndToppingId(branchId, request.getToppingId())) {
            throw new BaseException(ErrorCode.BRANCH_011);
        }
        Branch branch = getBranchOrThrow(branchId);
        Topping topping = getToppingOrThrow(request.getToppingId());
        BranchToppingAvailability availability = branchAvailabilityMapper.toToppingEntity(request, branch, topping);
        availability = branchToppingAvailabilityRepository.save(availability);
        log.info("Branch topping availability created: id={}, branchId={}, toppingId={}", availability.getId(), branchId, topping.getId());
        return branchAvailabilityMapper.toToppingResponse(availability);
    }

    @Override
    @Transactional
    public BranchToppingAvailabilityResponse updateToppingAvailability(String branchId, String id, UpdateBranchToppingAvailabilityRequest request) {
        accessScopeService.assertCanManageBranch(branchId);
        BranchToppingAvailability availability = getToppingAvailabilityOrThrow(branchId, id);
        Topping topping = null;
        if (request.getToppingId() != null) {
            if (branchToppingAvailabilityRepository.existsByBranchIdAndToppingIdAndIdNot(branchId, request.getToppingId(), id)) {
                throw new BaseException(ErrorCode.BRANCH_011);
            }
            topping = getToppingOrThrow(request.getToppingId());
        }
        branchAvailabilityMapper.updateToppingEntity(availability, request, topping);
        availability = branchToppingAvailabilityRepository.save(availability);
        log.info("Branch topping availability updated: id={}, branchId={}", availability.getId(), branchId);
        return branchAvailabilityMapper.toToppingResponse(availability);
    }

    @Override
    @Transactional
    public void deleteToppingAvailability(String branchId, String id) {
        accessScopeService.assertCanManageBranch(branchId);
        BranchToppingAvailability availability = getToppingAvailabilityOrThrow(branchId, id);
        branchToppingAvailabilityRepository.delete(availability);
        log.info("Branch topping availability deleted: id={}, branchId={}", id, branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchToppingAvailabilityResponse getToppingAvailability(String branchId, String id) {
        accessScopeService.assertCanAccessBranch(branchId);
        return branchAvailabilityMapper.toToppingResponse(getToppingAvailabilityOrThrow(branchId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchToppingAvailabilityResponse> getToppingAvailabilities(String branchId) {
        accessScopeService.assertCanAccessBranch(branchId);
        getBranchOrThrow(branchId);
        return branchToppingAvailabilityRepository.findByBranchId(branchId).stream()
                .map(branchAvailabilityMapper::toToppingResponse)
                .toList();
    }

    private Branch getBranchOrThrow(String branchId) {
        return branchRepository.findById(branchId).orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
    }

    private Product getProductOrThrow(String productId) {
        return productRepository.findById(productId).orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_001));
    }

    private Topping getToppingOrThrow(String toppingId) {
        return toppingRepository.findById(toppingId).orElseThrow(() -> new BaseException(ErrorCode.TOPPING_001));
    }

    private BranchProductAvailability getProductAvailabilityOrThrow(String branchId, String id) {
        return branchProductAvailabilityRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_008));
    }

    private BranchToppingAvailability getToppingAvailabilityOrThrow(String branchId, String id) {
        return branchToppingAvailabilityRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_010));
    }

    private void validateAvailabilityRange(LocalDateTime availableFrom, LocalDateTime availableTo) {
        if (availableFrom != null && availableTo != null && !availableFrom.isBefore(availableTo)) {
            throw new BaseException(ErrorCode.BRANCH_012);
        }
    }
}
