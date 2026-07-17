package com.hoandev.pinedrink.entity.dto.request.Branch;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBranchToppingAvailabilityRequest {
    private String toppingId;
    private Boolean available;

    @Size(max = 255, message = "Sold out reason must be at most 255 characters")
    private String soldOutReason;
}
