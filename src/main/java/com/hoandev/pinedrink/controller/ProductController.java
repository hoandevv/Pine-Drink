package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Product.CreateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductSummaryResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductVariantResponse;
import com.hoandev.pinedrink.service.ProductService;
import com.hoandev.pinedrink.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller xử lý các API liên quan đến quản lý sản phẩm.
 * Cung cấp các chức năng CRUD: tạo, cập nhật, xóa, tìm kiếm sản phẩm.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;

    /**
     * Tạo mới sản phẩm.
     *
     * @param request thông tin sản phẩm cần tạo
     * @return {@link ProductResponse} sản phẩm vừa được tạo
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PRODUCT_CREATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> create(@Valid @RequestBody CreateProductRequest request) {
        log.info("Creating product for categoryId={}", request.getCategoryId());
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Product created successfully"));
    }

    /**
     * Tạo mới sản phẩm kèm hình ảnh.
     *
     * @param request thông tin sản phẩm cần tạo
     * @param file    file hình ảnh (tùy chọn)
     * @return {@link ProductResponse} sản phẩm vừa được tạo
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_CREATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> createWithImage(
            @Valid @RequestPart("request") CreateProductRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Creating product with image for categoryId={}", request.getCategoryId());
        ProductResponse response = productService.create(request, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Product created successfully"));
    }

    /**
     * Cập nhật thông tin sản phẩm.
     *
     * @param id      mã sản phẩm
     * @param request thông tin cập nhật
     * @return {@link ProductResponse} sản phẩm sau khi cập nhật
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("Updating product: id={}", id);
        ProductResponse response = productService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Product updated successfully"));
    }

    /**
     * Cập nhật sản phẩm kèm hình ảnh.
     *
     * @param id      mã sản phẩm
     * @param request thông tin cập nhật
     * @param file    file hình ảnh mới (tùy chọn)
     * @return {@link ProductResponse} sản phẩm sau khi cập nhật
     */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> updateWithImage(
            @PathVariable String id,
            @Valid @RequestPart("request") UpdateProductRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Updating product with image: id={}", id);
        ProductResponse response = productService.update(id, request, file);
        return ResponseEntity.ok(BaseResponse.success(response, "Product updated successfully"));
    }

    /**
     * Cập nhật trạng thái sản phẩm.
     *
     * @param id      mã sản phẩm
     * @param request thông tin trạng thái mới
     * @return {@link ProductResponse} sản phẩm sau khi cập nhật trạng thái
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        log.info("Updating product status: id={}, status={}", id, request.getStatus());
        ProductResponse response = productService.updateStatus(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Product status updated successfully"));
    }

    /**
     * Xóa sản phẩm theo mã.
     *
     * @param id mã sản phẩm cần xóa
     * @return {@link Void} không trả về dữ liệu
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_DELETE')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Deleting product: id={}", id);
        productService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Product deleted successfully"));
    }

    /**
     * Lấy thông tin sản phẩm theo mã.
     *
     * @param id mã sản phẩm
     * @return {@link ProductResponse} thông tin sản phẩm
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ProductResponse>> getById(@PathVariable String id) {
        log.info("Getting product: id={}", id);
        ProductResponse response = productService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Product retrieved successfully"));
    }

    /**
     * Lấy danh sách biến thể đang hoạt động của tất cả sản phẩm.
     *
     * @return danh sách {@link ProductVariantResponse}
     */
    @GetMapping("/variants/active")
    public ResponseEntity<BaseResponse<java.util.List<ProductVariantResponse>>> getAllActiveVariants() {
        log.info("Getting active variants for active products");
        java.util.List<ProductVariantResponse> response = productVariantService.getAllActiveForProducts();
        return ResponseEntity.ok(BaseResponse.success(response, "Active product variants retrieved successfully"));
    }

    /**
     * Lấy danh sách sản phẩm có phân trang và bộ lọc.
     *
     * @param keyword    từ khóa tìm kiếm (tùy chọn)
     * @param categoryId mã danh mục (tùy chọn)
     * @param status     trạng thái sản phẩm (tùy chọn)
     * @param pageable   thông tin phân trang
     * @return {@link PageResponse} danh sách sản phẩm
     */
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<ProductResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting products: keyword={}, categoryId={}, status={}", keyword, categoryId, status);
        PageResponse<ProductResponse> response = productService.getAll(keyword, categoryId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Products retrieved successfully"));
    }

    /**
     * Lấy danh sách tóm tắt sản phẩm có phân trang và bộ lọc.
     *
     * @param keyword    từ khóa tìm kiếm (tùy chọn)
     * @param categoryId mã danh mục (tùy chọn)
     * @param status     trạng thái sản phẩm (tùy chọn)
     * @param pageable   thông tin phân trang
     * @return {@link PageResponse} danh sách tóm tắt sản phẩm
     */
    @GetMapping("/summaries")
    public ResponseEntity<BaseResponse<PageResponse<ProductSummaryResponse>>> getSummaries(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting product summaries: keyword={}, categoryId={}, status={}", keyword, categoryId, status);
        PageResponse<ProductSummaryResponse> response = productService.getSummaries(keyword, categoryId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Product summaries retrieved successfully"));
    }
}
