package com.hoandev.pinedrink.entity.dto.request.Order;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {

    @NotBlank(message = "Cancel reason is required")
    private String reason;
}
