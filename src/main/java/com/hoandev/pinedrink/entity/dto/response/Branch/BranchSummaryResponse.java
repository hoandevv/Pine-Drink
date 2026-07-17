package com.hoandev.pinedrink.entity.dto.response.Branch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchSummaryResponse {
    private String id;
    private String code;
    private String name;
    private String address;
    private String phone;
    private boolean supportsPickup;
    private boolean supportsDelivery;
    private int averagePreparationMinutes;
    private String status;
}
