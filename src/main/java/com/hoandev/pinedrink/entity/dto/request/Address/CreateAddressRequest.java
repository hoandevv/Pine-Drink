package com.hoandev.pinedrink.entity.dto.request.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAddressRequest {

    @NotBlank(message = "Receiver name is required")
    @Size(max = 100, message = "Receiver name must be at most 100 characters")
    private String receiverName;

    @NotBlank(message = "Receiver phone is required")
    @Size(max = 20, message = "Receiver phone must be at most 20 characters")
    private String receiverPhone;

    @NotBlank(message = "Address line is required")
    @Size(max = 255, message = "Address line must be at most 100 characters")

    private String addressLine;

    @Size(max = 100)
    private String ward;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String city;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private boolean isDefault;

}
