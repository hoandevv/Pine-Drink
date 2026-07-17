package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Product.CreateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductSummaryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {

    /**
     * Tạo mới sản phẩm.
     */
    ProductResponse create(CreateProductRequest request);

    /**
     * Tạo mới sản phẩm kèm hình ảnh.
     */
    ProductResponse create(CreateProductRequest request, MultipartFile imageFile);

    /**
     * Cập nhật thông tin sản phẩm.
     *
     * @param id      mã sản phẩm
     * @param request thông tin cập nhật
     * @return {@link ProductResponse} sản phẩm sau khi cập nhật
     */
    ProductResponse update(String id, UpdateProductRequest request);

    /**
     * Cập nhật sản phẩm kèm hình ảnh.
     *
     * @param id        mã sản phẩm
     * @param request   thông tin cập nhật
     * @param imageFile file hình ảnh mới (tùy chọn)
     * @return {@link ProductResponse} sản phẩm sau khi cập nhật
     */
    ProductResponse update(String id, UpdateProductRequest request, MultipartFile imageFile);

    /**
     * Cập nhật trạng thái sản phẩm.
     *
     * @param id      mã sản phẩm
     * @param request thông tin trạng thái mới
     * @return {@link ProductResponse} sản phẩm sau khi cập nhật
     */
    ProductResponse updateStatus(String id, UpdateProductStatusRequest request);

    /**
     * Xóa sản phẩm theo mã.
     *
     * @param id mã sản phẩm cần xóa
     */
    void delete(String id);

    /**
     * Lấy thông tin sản phẩm theo mã.
     *
     * @param id mã sản phẩm
     * @return {@link ProductResponse} thông tin sản phẩm
     */
    ProductResponse getById(String id);

    /**
     * Lấy danh sách tóm tắt sản phẩm có phân trang và bộ lọc.
     *
     * @param keyword    từ khóa tìm kiếm (tùy chọn)
     * @param categoryId mã danh mục (tùy chọn)
     * @param status     trạng thái (tùy chọn)
     * @param pageable   thông tin phân trang
     * @return {@link PageResponse} danh sách tóm tắt sản phẩm
     */
    PageResponse<ProductSummaryResponse> getSummaries(String keyword, String categoryId, String status, Pageable pageable);

    /**
     * Lấy danh sách sản phẩm chi tiết có phân trang và bộ lọc.
     *
     * @param keyword    từ khóa tìm kiếm (tùy chọn)
     * @param categoryId mã danh mục (tùy chọn)
     * @param status     trạng thái (tùy chọn)
     * @param pageable   thông tin phân trang
     * @return {@link PageResponse} danh sách sản phẩm
     */
    PageResponse<ProductResponse> getAll(String keyword, String categoryId, String status, Pageable pageable);
}
