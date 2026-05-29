package com.hoandev.pinedrink.entity.dto.request.Branch;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchRequest {

    @NotBlank(message = "Branch name is required")
    @Size(max = 255, message = "Branch name must be at most 255 characters")
    private String name;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @NotBlank(message = "Timezone is required")
    @Size(max = 50, message = "Timezone must be at most 50 characters")
    private String timezone = "Asia/Ho_Chi_Minh";

    private boolean supportsPickup = true;

    private boolean supportsDelivery = false;

    private int averagePreparationMinutes = 15;

    @NotBlank(message = "Brand ID is required")
    private String brandId;
}
