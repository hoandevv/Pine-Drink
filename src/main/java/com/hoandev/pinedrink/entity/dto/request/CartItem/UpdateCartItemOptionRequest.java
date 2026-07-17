package com.hoandev.pinedrink.entity.dto.request.CartItem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateCartItemOptionRequest {

    @NotBlank(message = "SugarLevel is required")
    private String sugarLevel;

    @NotBlank(message = "IceLevel is required")
    private String iceLevel;

    @Size(max = 255, message = "Note must not exceed 255 characters")
    private String note;

    @Valid
    @Builder.Default
    private List<CartItemToppingRequest> toppings = new ArrayList<>();
}
