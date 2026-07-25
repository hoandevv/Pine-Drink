package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.repository.custom.ProductRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, ProductRepositoryCustom {
   /**
    * Kiểm tra sự tồn tại của sản phẩm theo mã sản phẩm.
    */
    boolean existsByCode(String code);
    /**
     * Cập nhật trạng thái của sản phẩm theo ID danh mục và trạng thái hiện tại.
     *
     * @param categoryId ID của danh mục
     * @param currentStatus trạng thái hiện tại
     * @param productStatus trạng thái mới
     * @return số lượng bản ghi được cập nhật
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Product p
            SET p.status = :productStatus
            WHERE p.category.id = :categoryId
              AND p.status = :currentStatus
            """)
    int updateStatusByCategoryIdAndStatus(
            @Param("categoryId") String categoryId,
            @Param("currentStatus") String currentStatus,
            @Param("productStatus") String productStatus
    );
}
