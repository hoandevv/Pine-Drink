package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.repository.projection.InvoiceHeaderProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    /**
     * Tìm đơn hàng theo mã đơn hàng duy nhất.
     *
     * @param orderCode mã đơn hàng
     * @return Optional chứa Order nếu tồn tại
     */
    Optional<Order> findByOrderCode(String orderCode);

    @Query(value = """
            SELECT
                o.id AS orderId,
                o.order_code AS orderCode,
                o.customer_name AS customerName,
                o.subtotal_amount AS subtotalAmount,
                o.discount_amount AS discountAmount,
                o.total_amount AS totalAmount,
                COALESCE(o.completed_at, o.created_at) AS orderTime,
                b.name AS branchName,
                b.address AS branchAddress
            FROM od_order o
            JOIN ce_branch b ON b.id = o.branch_id
            WHERE o.id = :orderId
            """, nativeQuery = true)
    Optional<InvoiceHeaderProjection> findInvoiceHeaderByOrderId(@Param("orderId") String orderId);

    @Query(value = """
            SELECT
                o.id AS orderId,
                o.order_code AS orderCode,
                o.customer_name AS customerName,
                o.subtotal_amount AS subtotalAmount,
                o.discount_amount AS discountAmount,
                o.total_amount AS totalAmount,
                COALESCE(o.completed_at, o.created_at) AS orderTime,
                b.name AS branchName,
                b.address AS branchAddress
            FROM od_order o
            JOIN ce_branch b ON b.id = o.branch_id
            WHERE o.order_code = :orderCode
            """, nativeQuery = true)
    Optional<InvoiceHeaderProjection> findInvoiceHeaderByOrderCode(@Param("orderCode") String orderCode);

    /**
     * Lấy đơn hàng theo id với khóa PESSIMISTIC_WRITE để tránh race condition khi cập nhật.
     *
     * @param orderId id của đơn hàng
     * @return Optional chứa Order khóa để cập nhật
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") String orderId);
    
    /**
     * Lấy trang các đơn hàng của một khách hàng, sắp xếp theo thời gian tạo giảm dần.
     *
     * @param customerId id khách hàng
     * @param pageable thông tin phân trang
     * @return trang Order
     */
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
    
    /**
     * Lấy trang các đơn hàng của một chi nhánh, sắp xếp theo thời gian tạo giảm dần.
     *
     * @param branchId id chi nhánh
     * @param pageable thông tin phân trang
     * @return trang Order
     */
    Page<Order> findByBranchIdOrderByCreatedAtDesc(String branchId, Pageable pageable);
    
    /**
     * Lấy trang các đơn hàng của chi nhánh theo trạng thái, sắp xếp theo thời gian tạo giảm dần.
     *
     * @param branchId id chi nhánh
     * @param status trạng thái đơn hàng
     * @param pageable thông tin phân trang
     * @return trang Order
     */
    Page<Order> findByBranchIdAndStatusOrderByCreatedAtDesc(String branchId, String status, Pageable pageable);

    /**
     * Lấy trang các đơn hàng theo trạng thái, sắp xếp theo thời gian tạo giảm dần.
     *
     * @param status trạng thái đơn hàng
     * @param pageable thông tin phân trang
     * @return trang Order
     */
    Page<Order> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    /**
     * Lấy tất cả đơn hàng theo trang, sắp xếp theo thời gian tạo giảm dần.
     *
     * @param pageable thông tin phân trang
     * @return trang Order
     */
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Lấy danh sách đơn hàng có trạng thái cho trước và được tạo trước một thời điểm nhất định.
     * Thường dùng để tìm đơn hàng cũ cần xử lý (ví dụ: hết hạn).
     *
     * @param status trạng thái đơn hàng
     * @param createdAt mốc thời gian (tất cả đơn trước mốc này)
     * @return danh sách Order
     */
    List<Order> findByStatusAndCreatedAtBefore(String status, LocalDateTime createdAt);
}
