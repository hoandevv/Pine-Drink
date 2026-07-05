package com.hoandev.pinedrink.repository.custom;

import com.hoandev.pinedrink.repository.result.ProductCatalogResult;

import java.util.List;


public interface ProductRepositoryCustom {
    /**
     * Lấy danh sách sản phẩm dùng cho báo cáo danh mục sản phẩm.
     * Kết quả gồm thông tin sản phẩm, danh mục và danh sách biến thể đã gộp thành chuỗi.
     *
     * @param status trạng thái sản phẩm cần lọc, truyền null để lấy tất cả trạng thái
     * @param categoryId id danh mục cần lọc, truyền null để lấy tất cả danh mục
     * @return danh sách sản phẩm cho báo cáo, sắp xếp theo danh mục rồi tên sản phẩm
     */
    List<ProductCatalogResult> findProductCatalogReport(String status, String categoryId);
}
