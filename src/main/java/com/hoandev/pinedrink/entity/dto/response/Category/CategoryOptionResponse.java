package com.hoandev.pinedrink.entity.dto.response.Category;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryOptionResponse {
    private String id;
    private String code;
    private String name;
    private String imageUrl;
}
