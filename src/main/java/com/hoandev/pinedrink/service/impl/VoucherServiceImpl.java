package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.Voucher;
import com.hoandev.pinedrink.entity.VoucherBranch;
import com.hoandev.pinedrink.entity.dto.request.Voucher.CreateVoucherRequest;
import com.hoandev.pinedrink.entity.dto.request.Voucher.UpdateVoucherRequest;
import com.hoandev.pinedrink.entity.dto.request.Voucher.UpdateVoucherStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Voucher.VoucherResponse;
import com.hoandev.pinedrink.entity.enums.DiscountType;
import com.hoandev.pinedrink.entity.enums.EntityStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.VoucherMapper;
import com.hoandev.pinedrink.repository.BranchRepository;
import com.hoandev.pinedrink.repository.VoucherBranchRepository;
import com.hoandev.pinedrink.repository.VoucherRepository;
import com.hoandev.pinedrink.repository.VoucherUsageRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherBranchRepository voucherBranchRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final BranchRepository branchRepository;
    private final VoucherMapper voucherMapper;
    private final AccessScopeService accessScopeService;

    @Override
    @Transactional
    public VoucherResponse create(CreateVoucherRequest request) {
        accessScopeService.assertSystemAccess();
        String normalizedCode = normalizeCode(request.getCode());
        validateVoucherRules(request.getDiscountType(), request.getDiscountValue(), request.getMaxDiscountAmount(),
                request.getMinOrderAmount(), request.getUsageLimit(), request.getUsageLimitPerCustomer(),
                request.getStartAt(), request.getEndAt());
        if (voucherRepository.existsByCode(normalizedCode)) {
            throw new BaseException(ErrorCode.VOUCHER_002);
        }

        List<String> branchIds = normalizeBranchIds(request.getBranchIds());
        List<Branch> branches = resolveBranches(branchIds);

        Voucher voucher = voucherMapper.toEntity(request, normalizedCode);
        voucher.setStatus(EntityStatus.ACTIVE.getValue());
        voucher = voucherRepository.save(voucher);
        replaceBranchScope(voucher, branches);
        log.info("Voucher created: id={}, code={}", voucher.getId(), voucher.getCode());
        return voucherMapper.toResponse(voucher, branchIds);
    }

    @Override
    @Transactional
    public VoucherResponse update(String id, UpdateVoucherRequest request) {
        accessScopeService.assertSystemAccess();
        Voucher voucher = getVoucherOrThrow(id);
        String normalizedCode = normalizeCode(request.getCode());
        validateVoucherRules(request.getDiscountType(), request.getDiscountValue(), request.getMaxDiscountAmount(),
                request.getMinOrderAmount(), request.getUsageLimit(), request.getUsageLimitPerCustomer(),
                request.getStartAt(), request.getEndAt());
        if (voucherRepository.existsByCodeAndIdNot(normalizedCode, id)) {
            throw new BaseException(ErrorCode.VOUCHER_002);
        }

        List<String> branchIds = normalizeBranchIds(request.getBranchIds());
        List<Branch> branches = resolveBranches(branchIds);

        voucherMapper.updateEntity(voucher, request, normalizedCode);
        voucher = voucherRepository.save(voucher);
        replaceBranchScope(voucher, branches);
        log.info("Voucher updated: id={}, code={}", voucher.getId(), voucher.getCode());
        return voucherMapper.toResponse(voucher, branchIds);
    }

    @Override
    @Transactional
    public VoucherResponse updateStatus(String id, UpdateVoucherStatusRequest request) {
        accessScopeService.assertSystemAccess();
        if (request.getStatus() == EntityStatus.DELETED) {
            throw new BaseException(ErrorCode.VOUCHER_006);
        }

        Voucher voucher = getVoucherOrThrow(id);
        voucher.setStatus(request.getStatus().getValue());
        voucher = voucherRepository.save(voucher);
        List<String> branchIds = getBranchIdsByVoucherId(voucher.getId());
        log.info("Voucher status updated: id={}, code={}, status={}", voucher.getId(), voucher.getCode(), voucher.getStatus());
        return voucherMapper.toResponse(voucher, branchIds);
    }

    @Override
    @Transactional
    public void delete(String id) {
        accessScopeService.assertSystemAccess();
        Voucher voucher = getVoucherOrThrow(id);
        if (EntityStatus.DELETED.getValue().equals(voucher.getStatus())) {
            throw new BaseException(ErrorCode.VOUCHER_006);
        }

        voucher.setStatus(EntityStatus.DELETED.getValue());
        voucherRepository.save(voucher);
        log.info("Voucher deleted (soft): id={}, code={}", voucher.getId(), voucher.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getById(String id) {
        Voucher voucher = getVoucherOrThrow(id);
        return voucherMapper.toResponse(voucher, getBranchIdsByVoucherId(voucher.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> getAll(String keyword, String status, String discountType,
                                                String branchId, LocalDateTime activeAt, Pageable pageable) {
        String normalizedBranchId = normalizeOptional(branchId);
        if (normalizedBranchId != null) {
            branchRepository.findById(normalizedBranchId).orElseThrow(branchNotFound());
        }

        Page<Voucher> vouchers = voucherRepository.search(
                normalizeOptional(keyword),
                normalizeOptional(status),
                normalizeDiscountType(discountType),
                normalizedBranchId,
                activeAt,
                pageable
        );

        Map<String, List<String>> branchIdsByVoucherId = getBranchIdsByVoucherIds(
                vouchers.getContent().stream().map(Voucher::getId).toList()
        );

        List<VoucherResponse> content = vouchers.getContent().stream()
                .map(voucher -> voucherMapper.toResponse(
                        voucher,
                        branchIdsByVoucherId.getOrDefault(voucher.getId(), Collections.emptyList())
                ))
                .toList();
        return PageResponse.from(vouchers, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> getAvailableForCustomer(String branchId, Pageable pageable) {
        String normalizedBranchId = normalizeOptional(branchId);
        if (normalizedBranchId == null) {
            throw new BaseException(ErrorCode.COM_004);
        }
        branchRepository.findById(normalizedBranchId).orElseThrow(branchNotFound());

        Page<Voucher> vouchers = voucherRepository.search(
                null,
                EntityStatus.ACTIVE.getValue(),
                null,
                normalizedBranchId,
                LocalDateTime.now(),
                pageable
        );

        Map<String, List<String>> branchIdsByVoucherId = getBranchIdsByVoucherIds(
                vouchers.getContent().stream().map(Voucher::getId).toList()
        );

        List<VoucherResponse> content = vouchers.getContent().stream()
                .map(voucher -> voucherMapper.toResponse(
                        voucher,
                        branchIdsByVoucherId.getOrDefault(voucher.getId(), Collections.emptyList())
                ))
                .toList();

        return PageResponse.from(vouchers, content);
    }

    private Voucher getVoucherOrThrow(String id) {
        return voucherRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.VOUCHER_001));
    }

    private void validateVoucherRules(DiscountType discountType, BigDecimal discountValue, BigDecimal maxDiscountAmount,
                                      BigDecimal minOrderAmount, Integer usageLimit, Integer usageLimitPerCustomer,
                                      LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new BaseException(ErrorCode.VOUCHER_003);
        }
        if (discountType == null || discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BaseException(ErrorCode.VOUCHER_004);
        }
        if (discountType == DiscountType.PERCENTAGE && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BaseException(ErrorCode.VOUCHER_004);
        }
        if (discountType == DiscountType.PERCENTAGE
                && maxDiscountAmount != null
                && maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BaseException(ErrorCode.VOUCHER_004);
        }
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BaseException(ErrorCode.VOUCHER_004);
        }
        if (usageLimit != null && usageLimit < 1) {
            throw new BaseException(ErrorCode.VOUCHER_004);
        }
        if (usageLimitPerCustomer != null && usageLimitPerCustomer < 1) {
            throw new BaseException(ErrorCode.VOUCHER_004);
        }
    }

    private String normalizeCode(String code) {
        String normalizedCode = normalizeOptional(code);
        if (normalizedCode == null) {
            throw new BaseException(ErrorCode.COM_001);
        }
        return normalizedCode.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeDiscountType(String discountType) {
        String normalized = normalizeOptional(discountType);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private List<String> normalizeBranchIds(List<String> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String branchId : branchIds) {
            String trimmed = normalizeOptional(branchId);
            if (trimmed == null || !normalized.add(trimmed)) {
                throw new BaseException(ErrorCode.VOUCHER_007);
            }
        }
        return new ArrayList<>(normalized);
    }

    private List<Branch> resolveBranches(List<String> branchIds) {
        if (branchIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Branch> branches = branchRepository.findAllById(branchIds);
        if (branches.size() != branchIds.size()) {
            throw branchNotFound().get();
        }

        Map<String, Branch> branchById = new LinkedHashMap<>();
        for (Branch branch : branches) {
            branchById.put(branch.getId(), branch);
        }

        List<Branch> orderedBranches = new ArrayList<>(branchIds.size());
        for (String branchId : branchIds) {
            Branch branch = branchById.get(branchId);
            if (branch == null) {
                throw branchNotFound().get();
            }
            orderedBranches.add(branch);
        }
        return orderedBranches;
    }

    private void replaceBranchScope(Voucher voucher, List<Branch> branches) {
        voucherBranchRepository.deleteByVoucherId(voucher.getId());
        voucherBranchRepository.flush();
        if (branches.isEmpty()) {
            return;
        }

        List<VoucherBranch> voucherBranches = branches.stream().map(branch -> {
            VoucherBranch voucherBranch = new VoucherBranch();
            voucherBranch.setVoucher(voucher);
            voucherBranch.setBranch(branch);
            return voucherBranch;
        }).toList();
        voucherBranchRepository.saveAll(voucherBranches);
    }

    private List<String> getBranchIdsByVoucherId(String voucherId) {
        return getBranchIdsByVoucherIds(List.of(voucherId)).getOrDefault(voucherId, Collections.emptyList());
    }

    private Map<String, List<String>> getBranchIdsByVoucherIds(Collection<String> voucherIds) {
        if (voucherIds == null || voucherIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> branchIdsByVoucherId = new LinkedHashMap<>();
        voucherBranchRepository.findByVoucherIdIn(voucherIds).forEach(voucherBranch -> {
            String voucherId = voucherBranch.getVoucher().getId();
            branchIdsByVoucherId.computeIfAbsent(voucherId, ignored -> new ArrayList<>())
                    .add(voucherBranch.getBranch().getId());
        });
        return branchIdsByVoucherId;
    }

    private Supplier<BaseException> branchNotFound() {
        return () -> new BaseException(ErrorCode.BRANCH_001);
    }
}
