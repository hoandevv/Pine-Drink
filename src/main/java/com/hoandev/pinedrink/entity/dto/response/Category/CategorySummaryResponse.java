package com.hoandev.pinedrink.entity.dto.response.Category;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategorySummaryResponse {
    private String id;
    private String code;
    private String name;
    private String imageUrl;
    private int displayOrder;
    private String status;
}
