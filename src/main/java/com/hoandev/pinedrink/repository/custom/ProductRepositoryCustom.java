package com.hoandev.pinedrink.repository.custom;

import com.hoandev.pinedrink.entity.dto.response.Product.ProductResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductSummaryResponse;
import com.hoandev.pinedrink.entity.dto.report.ProductCatalogReportItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface ProductRepositoryCustom {
    /**
     * Tìm thông tin sản phẩm theo ID.
     *
     * @param id ID của sản phẩm
     * @return Optional chứa thông tin sản phẩm nếu tìm thấy, ngược lại là Optional.empty()
     */
    Optional<ProductResponse> findProductResponseById(String id);
    /**
     * Tìm kiếm thông tin sản phẩm.
     *
     * @param keyword từ khóa tìm kiếm
     * @param categoryId id danh mục
     * @param status trạng thái sản phẩm
     * @param categoryStatus trạng thái danh mục
     * @param pageable thông tin phân trang
     * @return trang chứa kết quả tìm kiếm
     */
    Page<ProductResponse> searchProductResponses(
            String keyword, String categoryId, String status, String categoryStatus, Pageable pageable);
    /**
     * Tìm kiếm thông tin tóm tắt sản phẩm.
     *
     * @param keyword từ khóa tìm kiếm
     * @param categoryId id danh mục
     * @param status trạng thái sản phẩm
     * @param pageable thông tin phân trang
     * @return trang chứa kết quả tìm kiếm
     */
    Page<ProductSummaryResponse> searchProductSummaryResponses(
            String keyword, String categoryId, String status, Pageable pageable);

    /**
     * Lấy danh sách sản phẩm dùng cho báo cáo danh mục sản phẩm.
     * Kết quả gồm thông tin sản phẩm, danh mục và danh sách biến thể đã gộp thành chuỗi.
     *
     * @param status trạng thái sản phẩm cần lọc, truyền null để lấy tất cả trạng thái
     * @param categoryId id danh mục cần lọc, truyền null để lấy tất cả danh mục
     * @return danh sách sản phẩm cho báo cáo, sắp xếp theo danh mục rồi tên sản phẩm
     */
    List<ProductCatalogReportItemDto> findProductCatalogReport(String status, String categoryId, LocalDateTime fromDate, LocalDateTime toDate);
}
