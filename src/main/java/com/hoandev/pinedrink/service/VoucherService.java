package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Voucher.CreateVoucherRequest;
import com.hoandev.pinedrink.entity.dto.request.Voucher.UpdateVoucherRequest;
import com.hoandev.pinedrink.entity.dto.request.Voucher.UpdateVoucherStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Voucher.VoucherResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
/**
 * Service interface for managing vouchers.
 */
public interface VoucherService {
    /**
     * Creates a new voucher.
     *
     * @param request the request containing voucher details
     * @return the created voucher response
     */
    VoucherResponse create(CreateVoucherRequest request);

    /**
     * Updates an existing voucher.
     *
     * @param id      the ID of the voucher to update
     * @param request the request containing updated voucher details
     * @return the updated voucher response
     */
    VoucherResponse update(String id, UpdateVoucherRequest request);

    /**
     * Updates the status of a voucher.
     *
     * @param id      the ID of the voucher to update
     * @param request the request containing the new status
     * @return the updated voucher response
     */
    VoucherResponse updateStatus(String id, UpdateVoucherStatusRequest request);
    /**
     * Deletes a voucher.
     *
     * @param id the ID of the voucher to delete
     */
    void delete(String id);
    /**
     * Retrieves a voucher by its ID.
     *
     * @param id the ID of the voucher to retrieve
     * @return the voucher response
     */
    VoucherResponse getById(String id);
    /**
     * Retrieves all vouchers with pagination.
     *
     * @param keyword      the keyword to search for
     * @param status       the status of the vouchers to retrieve
     * @param discountType the discount type of the vouchers to retrieve
     * @param branchId     the ID of the branch to filter by
     * @param activeAt     the date and time when the vouchers are active
     * @param pageable     the pagination information
     * @return the page of voucher responses
     */
    PageResponse<VoucherResponse> getAll(String keyword, String status, String discountType,
                                         String branchId, LocalDateTime activeAt, Pageable pageable);
}
