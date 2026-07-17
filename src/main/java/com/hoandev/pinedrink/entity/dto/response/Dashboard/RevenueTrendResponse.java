package com.hoandev.pinedrink.entity.dto.response.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueTrendResponse {
    private LocalDate date;
    private BigDecimal revenue;
    private long orders;
    private BigDecimal averageOrderValue;
}
