package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.response.Dashboard.DashboardDataResponse;

import java.time.LocalDate;

public interface DashboardService {
    /**
     *  Lấy toàn bộ thông tin trên 1 request
     * */
    DashboardDataResponse getAllData(LocalDate fromDate, LocalDate toDate, String branchId, Integer limit);
}
