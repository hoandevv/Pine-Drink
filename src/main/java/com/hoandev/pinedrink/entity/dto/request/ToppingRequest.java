package com.hoandev.pinedrink.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToppingRequest {
    @NotBlank
    private String brandId;

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotNull
    private BigDecimal price;

    private String imageUrl;
}
