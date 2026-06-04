package com.hoandev.pinedrink.entity.dto.request.Category;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCategoryRequest {

    @Size(max = 150, message = "Category name must be at most 150 characters")
    private String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @Size(max = 1000, message = "Image URL must be at most 1000 characters")
    private String imageUrl;

    @Min(value = 0, message = "Display order must be greater than or equal to 0")
    private Integer displayOrder;
}
