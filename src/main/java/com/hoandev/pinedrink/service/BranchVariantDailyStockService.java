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
     * Retrieves daily stock information for a specific branch and date.
     * @param branchId the ID of the branch
     * @param stockDate the date of the stock information
     * @return a list of daily stock responses
     */
    List<DailyStockResponse> getByBranchAndDate(String branchId, LocalDate stockDate);
    /**
     * Retrieves public daily stock information for a specific branch and date.
     * @param branchId the ID of the branch
     * @param stockDate the date of the stock information
     * @return a list of daily stock responses
     */
    List<DailyStockResponse> getPublicByBranchAndDate(String branchId, LocalDate stockDate);
    /**
     * Sets the quota for a specific daily stock.
     * @param request the set quota request
     * @return the daily stock response
     */
    DailyStockResponse setQuota(SetDailyStockQuotaRequest request);
    /**
     * Updates the quota for a specific daily stock.
     * @param dailyStockId the ID of the daily stock
     * @param request the update request
     * @return the updated daily stock response
     */
    DailyStockResponse updateQuota(String dailyStockId, UpdateDailyStockQuotaRequest request);
    /**
     * Copies the quota from one daily stock to another.
     * @param request the copy request
     * @return the response containing the copied quota information
     */
    CopyDailyStockQuotaResponse copyQuota(CopyDailyStockQuotaRequest request);
    /**
     * Retrieves logs for a specific daily stock.
     * @param dailyStockId the ID of the daily stock
     * @param pageable the pagination information
     * @return a page of daily stock log responses
     */
    PageResponse<DailyStockLogResponse> getLogs(String dailyStockId, Pageable pageable);
    /**
     * Retrieves logs for a specific order.
     * @param orderId the ID of the order
     * @param pageable the pagination information
     * @return a page of daily stock log responses
     */
    PageResponse<DailyStockLogResponse> getLogsByOrder(String orderId, Pageable pageable);
    /**
     * Retrieves the available quantity for a specific branch, variant, and date.
     * @param branchId the ID of the branch
     * @param variantId the ID of the variant
     * @param stockDate the date of the stock information
     * @return the available quantity
     */
    int getAvailableQuantity(String branchId, String variantId, LocalDate stockDate);
    /**
     * Reserves a certain quantity of a variant for a specific branch and date.
     * @param branchId the ID of the branch
     * @param variantId the ID of the variant
     * @param stockDate the date of the stock information
     * @param quantity the quantity to reserve
     * @param orderId the ID of the order
     */
    void reserve(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId);
    /**
     * Confirms the sale of a certain quantity of a variant for a specific branch and date.
     * @param branchId the ID of the branch
     * @param variantId the ID of the variant
     * @param stockDate the date of the stock information
     * @param quantity the quantity to confirm
     * @param orderId the ID of the order
     */
    void confirmSold(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId);
    /**
     * Releases a certain quantity of a variant for a specific branch and date.
     * @param branchId the ID of the branch
     * @param variantId the ID of the variant
     * @param stockDate the date of the stock information
     * @param quantity the quantity to release
     * @param orderId the ID of the order
     */
    void release(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId);
}
