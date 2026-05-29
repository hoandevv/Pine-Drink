package com.hoandev.pinedrink.entity.dto.request.Address;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCusAddress {

    @NotBlank(message = "Receiver name is required")
    @Size(max = 100)
    private String receiverName;

    @NotBlank(message = "Receiver phone is required")
    @Size(max = 20)
    private String receiverPhone;

    @NotBlank(message = "Address line is required")
    @Size(max = 255)
    private String addressLine;

    @Size(max = 100)
    private String ward;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String city;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @JsonProperty("isDefault")
    private boolean isDefault;
}
