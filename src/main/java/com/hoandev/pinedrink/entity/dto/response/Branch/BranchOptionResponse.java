package com.hoandev.pinedrink.entity.dto.response.Branch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchOptionResponse {
    private String id;
    private String code;
    private String name;
    private String address;
    private boolean supportsPickup;
    private boolean supportsDelivery;
    private int averagePreparationMinutes;
}
