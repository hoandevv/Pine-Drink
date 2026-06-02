package com.hoandev.pinedrink.entity.dto.request.Topping;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateToppingRequest {

    @NotBlank(message = "Topping name is required")
    @Size(max = 150, message = "Topping name must be at most 150 characters")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @Size(max = 1000, message = "Image URL must be at most 1000 characters")
    private String imageUrl;

    @Size(max = 100, message = "Group name must be at most 100 characters")
    private String groupName;
}
