package com.hoandev.pinedrink.entity.dto.response.Topping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToppingResponse {

    private String id;
    private String code;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String groupName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
