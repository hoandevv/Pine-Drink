package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private String id;
    private String brandId;
    private String code;
    private String name;
    private String description;
    private String imageUrl;
    private int displayOrder;
    private String status;
}
