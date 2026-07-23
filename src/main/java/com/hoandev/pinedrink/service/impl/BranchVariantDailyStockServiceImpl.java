package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.*;
import com.hoandev.pinedrink.entity.dto.request.DailyStock.CopyDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.request.DailyStock.SetDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.request.DailyStock.UpdateDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.CopyDailyStockQuotaResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.DailyStockLogResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.DailyStockResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.enums.BranchVariantStockActionType;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.DailyStockMapper;
import com.hoandev.pinedrink.repository.*;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.BranchVariantDailyStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchVariantDailyStockServiceImpl implements BranchVariantDailyStockService {
    private final BranchVariantDailyStockRepository stockRepository;
    private final BranchVariantStockLogRepository logRepository;
    private final BranchRepository branchRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final AccessScopeService accessScopeService;
    private final DailyStockMapper dailyStockMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DailyStockResponse> getByBranchAndDate(String branchId, LocalDate stockDate) {
        accessScopeService.assertCanAccessBranch(branchId);
        return stockRepository.findByBranchIdAndStockDate(branchId, stockDate).stream()
                .map(dailyStockMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyStockResponse> getPublicByBranchAndDate(String branchId, LocalDate stockDate) {
        return stockRepository.findByBranchIdAndStockDate(branchId, stockDate).stream()
                .map(dailyStockMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DailyStockResponse setQuota(SetDailyStockQuotaRequest request) {
        accessScopeService.assertCanManageBranch(request.getBranchId());
        Branch branch = branchRepository.findById(request.getBranchId()).orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
        ProductVariant variant = productVariantRepository.findById(request.getVariantId()).orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_007));

        BranchVariantDailyStock stock = stockRepository.findForUpdate(request.getBranchId(), request.getVariantId(),request.getStockDate()).orElse(null);
        boolean isNew = stock == null;
        if (isNew){
          stock = createStock(branch,variant, request.getStockDate());
        }

        BranchVariantStockActionType actionType = stock.getId() == null ? BranchVariantStockActionType.SET_QUOTA : BranchVariantStockActionType.ADJUST;
        validateDailyQuantity(request.getDailyQuantity(), stock);
        StockSnapshot before = StockSnapshot.from(stock);
        stock.setDailyQuantity(request.getDailyQuantity());
        stock = stockRepository.save(stock);
        StockSnapshot after = StockSnapshot.from(stock);
        saveLogIfChanged(stock, null, actionType, before, after, request.getReason());
        return dailyStockMapper.toResponse(stock);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyStockResponse getById(String dailyStockId) {
        BranchVariantDailyStock stock = stockRepository.findById(dailyStockId)
                .orElseThrow(() -> new BaseException(ErrorCode.DAILY_STOCK_001));
        accessScopeService.assertCanAccessBranch(stock.getBranch().getId());
        return dailyStockMapper.toResponse(stock);
    }

    @Override
    @Transactional
    public DailyStockResponse updateQuota(String dailyStockId, UpdateDailyStockQuotaRequest request) {
        BranchVariantDailyStock stock = stockRepository.findByIdForUpdate(dailyStockId).orElseThrow(() -> new BaseException(ErrorCode.DAILY_STOCK_001));
        accessScopeService.assertCanManageBranch(stock.getBranch().getId());
        validateDailyQuantity(request.getDailyQuantity(), stock);
        StockSnapshot before = StockSnapshot.from(stock);
        stock.setDailyQuantity(request.getDailyQuantity());
        stock = stockRepository.save(stock);
        StockSnapshot after = StockSnapshot.from(stock);
        saveLogIfChanged(stock, null, BranchVariantStockActionType.ADJUST, before, after, request.getReason());
        return dailyStockMapper.toResponse(stock);
    }
    /**
     * Copies daily stock quota from one date to another.
     * @param request the request containing the copy details
     * @return the response containing the copy results
     */
    @Override
    @Transactional
    public CopyDailyStockQuotaResponse copyQuota(CopyDailyStockQuotaRequest request) {
        accessScopeService.assertCanManageBranch(request.getBranchId());
        if (request.getSourceDate().equals(request.getTargetDate())) {
            throw new BaseException(ErrorCode.COM_004);
        }

        Branch branch = branchRepository.findById(request.getBranchId()).orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
        List<BranchVariantDailyStock> sourceStocks = stockRepository.findByBranchIdAndStockDate(request.getBranchId(), request.getSourceDate());
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        for (BranchVariantDailyStock source : sourceStocks) {
            BranchVariantDailyStock target = stockRepository.findForUpdate(request.getBranchId(), source.getVariant().getId(), request.getTargetDate()).orElse(null);
            if (target != null && !request.isOverwrite()) {
                skippedCount++;
                continue;
            }
            if (target == null) {
                target = createStock(branch, source.getVariant(), request.getTargetDate());
                StockSnapshot before = StockSnapshot.from(target);
                target.setDailyQuantity(source.getDailyQuantity());
                target = stockRepository.save(target);
                saveLogIfChanged(target, null, BranchVariantStockActionType.COPY_QUOTA, before, StockSnapshot.from(target), resolveCopyReason(request));
                createdCount++;
                continue;
            }
            if (source.getDailyQuantity() < target.getSoldQuantity() + target.getReservedQuantity()) {
                skippedCount++;
                continue;
            }
            StockSnapshot before = StockSnapshot.from(target);
            target.setDailyQuantity(source.getDailyQuantity());
            target = stockRepository.save(target);
            saveLogIfChanged(target, null, BranchVariantStockActionType.COPY_QUOTA, before, StockSnapshot.from(target), resolveCopyReason(request));
            updatedCount++;
        }

        return CopyDailyStockQuotaResponse.builder()
                .branchId(request.getBranchId())
                .sourceDate(request.getSourceDate())
                .targetDate(request.getTargetDate())
                .overwrite(request.isOverwrite())
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DailyStockLogResponse> getLogs(String dailyStockId, Pageable pageable) {
        BranchVariantDailyStock stock = stockRepository.findById(dailyStockId).orElseThrow(() -> new BaseException(ErrorCode.DAILY_STOCK_001));
        accessScopeService.assertCanAccessBranch(stock.getBranch().getId());
        Page<BranchVariantStockLog> logs = logRepository.findByDailyStockId(dailyStockId, pageable);
        return PageResponse.from(logs, logs.getContent().stream().map(dailyStockMapper::toLogResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DailyStockLogResponse> getLogsByOrder(String orderId, Pageable pageable) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));
        accessScopeService.assertCanAccessBranch(order.getBranch().getId());
        Page<BranchVariantStockLog> logs = logRepository.findByOrderId(orderId, pageable);
        return PageResponse.from(logs, logs.getContent().stream().map(dailyStockMapper::toLogResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableQuantity(String branchId, String variantId, LocalDate stockDate) {
        accessScopeService.assertCanAccessBranch(branchId);
        return stockRepository.findByBranchIdAndVariantIdAndStockDate(branchId, variantId, stockDate)
                .map(this::available)
                .orElse(0);
    }

    @Override
    @Transactional
    public void reserve(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId) {
        changeReservation(branchId, variantId, stockDate, quantity, orderId, BranchVariantStockActionType.RESERVE);
    }

    @Override
    @Transactional
    public void confirmSold(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId) {
        changeReservation(branchId, variantId, stockDate, quantity, orderId, BranchVariantStockActionType.SOLD);
    }

    @Override
    @Transactional
    public void release(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId) {
        changeReservation(branchId, variantId, stockDate, quantity, orderId, BranchVariantStockActionType.RELEASE);
    }
    /**
     * Changes the reservation status for a branch variant daily stock.
     * @param branchId the ID of the branch
     * @param variantId the ID of the variant
     * @param stockDate the date of the stock information
     * @param quantity the quantity to change
     * @param orderId the order ID
     * @param actionType the action type
     */
    private void changeReservation(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId, BranchVariantStockActionType actionType) {
        if (quantity <= 0) throw new BaseException(ErrorCode.DAILY_STOCK_002);
        BranchVariantDailyStock stock = stockRepository.findForUpdate(branchId, variantId, stockDate).orElseThrow(() -> new BaseException(ErrorCode.DAILY_STOCK_001));
        StockSnapshot before = StockSnapshot.from(stock);
        if (actionType == BranchVariantStockActionType.RESERVE) {
            if (available(stock) < quantity) throw new BaseException(ErrorCode.DAILY_STOCK_003);
            stock.setReservedQuantity(stock.getReservedQuantity() + quantity);
        } else if (actionType == BranchVariantStockActionType.SOLD) {
            if (stock.getReservedQuantity() < quantity) throw new BaseException(ErrorCode.DAILY_STOCK_004);
            stock.setReservedQuantity(stock.getReservedQuantity() - quantity);
            stock.setSoldQuantity(stock.getSoldQuantity() + quantity);
        } else if (actionType == BranchVariantStockActionType.RELEASE) {
            if (stock.getReservedQuantity() < quantity) throw new BaseException(ErrorCode.DAILY_STOCK_004);
            stock.setReservedQuantity(stock.getReservedQuantity() - quantity);
        }
        stock = stockRepository.save(stock);
        saveLog(stock, orderId, actionType, quantity, before, StockSnapshot.from(stock), null);
    }

    private BranchVariantDailyStock createStock(Branch branch, ProductVariant variant, LocalDate stockDate) {
        BranchVariantDailyStock stock = new BranchVariantDailyStock();
        stock.setBranch(branch);
        stock.setVariant(variant);
        stock.setStockDate(stockDate);
        return stock;
    }

    private void validateDailyQuantity(int dailyQuantity, BranchVariantDailyStock stock) {
        if (dailyQuantity < stock.getSoldQuantity() + stock.getReservedQuantity()) {
            throw new BaseException(ErrorCode.DAILY_STOCK_002);
        }
    }
    /**
     * Saves a log entry for a branch variant stock change.
     * @param stock the branch variant daily stock
     * @param orderId the order ID
     * @param actionType the action type
     * @param quantity the quantity of the change
     * @param before the snapshot before the change
     * @param after the snapshot after the change
     * @param reason the reason for the change
     */
    private void saveLog(BranchVariantDailyStock stock, String orderId, BranchVariantStockActionType actionType, int quantity, StockSnapshot before, StockSnapshot after, String reason) {
        if (quantity <= 0) {
            return;
        }
        BranchVariantStockLog log = new BranchVariantStockLog();
        log.setDailyStock(stock);
        log.setActionType(actionType.getValue());
        log.setQuantity(quantity);
        log.setBeforeDailyQuantity(before.dailyQuantity);
        log.setAfterDailyQuantity(after.dailyQuantity);
        log.setBeforeSoldQuantity(before.soldQuantity);
        log.setAfterSoldQuantity(after.soldQuantity);
        log.setBeforeReservedQuantity(before.reservedQuantity);
        log.setAfterReservedQuantity(after.reservedQuantity);
        log.setReason(reason);
        if (orderId != null) {
            orderRepository.findById(orderId).ifPresent(log::setOrder);
        }
        logRepository.save(log);
    }
    /**
     * Saves a log entry if any of the stock quantities have changed.
     * @param stock the branch variant daily stock
     * @param orderId the order ID
     * @param actionType the action type
     * @param before the snapshot before the change
     * @param after the snapshot after the change
     * @param reason the reason for the change
     */
    private void saveLogIfChanged(BranchVariantDailyStock stock, String orderId, BranchVariantStockActionType actionType, StockSnapshot before, StockSnapshot after, String reason) {
        int changedQuantity = Math.abs(after.dailyQuantity - before.dailyQuantity)
                + Math.abs(after.soldQuantity - before.soldQuantity)
                + Math.abs(after.reservedQuantity - before.reservedQuantity);
        saveLog(stock, orderId, actionType, changedQuantity, before, after, reason);
    }
    /**
     * Resolves the reason for copying daily stock quota.
     * @param request the request containing the copy details
     * @return the resolved reason
     */
    private String resolveCopyReason(CopyDailyStockQuotaRequest request) {
        if (request.getReason() != null && !request.getReason().isBlank()) {
            return request.getReason();
        }
        return "Copy quota from " + request.getSourceDate();
    }

    private int available(BranchVariantDailyStock stock) {
        return stock.getDailyQuantity() - stock.getSoldQuantity() - stock.getReservedQuantity();
    }

    private record StockSnapshot(int dailyQuantity, int soldQuantity, int reservedQuantity) {
        static StockSnapshot from(BranchVariantDailyStock stock) {
            return new StockSnapshot(stock.getDailyQuantity(), stock.getSoldQuantity(), stock.getReservedQuantity());
        }
    }
}
