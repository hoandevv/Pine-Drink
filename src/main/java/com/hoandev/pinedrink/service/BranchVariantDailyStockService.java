package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.DailyStock.SetDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.request.DailyStock.CopyDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.request.DailyStock.UpdateDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.CopyDailyStockQuotaResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.DailyStockLogResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.DailyStockResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
/**
 * Service interface for managing branch variant daily stock.
 */
public interface BranchVariantDailyStockService {
    /**
     * Retrieves daily stock infor     * @param branchId the ID of the branch
     * @param stockDate the date of the stock information
     * @return a list of daily stock responses
        mation for a specific branch and date.
     * */
    List<DailyStockResponse> getByBranchAndDate(String branchId, LocalDate stockDate);
    DailyStockResponse setQuota(SetDailyStockQuotaRequest request);
    DailyStockResponse updateQuota(String dailyStockId, UpdateDailyStockQuotaRequest request);
    CopyDailyStockQuotaResponse copyQuota(CopyDailyStockQuotaRequest request);
    PageResponse<DailyStockLogResponse> getLogs(String dailyStockId, Pageable pageable);
    PageResponse<DailyStockLogResponse> getLogsByOrder(String orderId, Pageable pageable);
    int getAvailableQuantity(String branchId, String variantId, LocalDate stockDate);
    void reserve(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId);
    void confirmSold(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId);
    void release(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId);
}
