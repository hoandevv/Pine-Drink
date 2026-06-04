package com.hoandev.pinedrink.entity.dto.request.Branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchToppingAvailabilityRequest {

    @NotBlank(message = "Topping id is required")
    private String toppingId;

    @Builder.Default
    private boolean available = true;

    @Size(max = 255, message = "Sold out reason must be at most 255 characters")
    private String soldOutReason;
}
