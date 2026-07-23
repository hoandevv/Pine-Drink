-- Pine Drink - Stored procedure phân tích dashboard
--
-- Mục đích:
--   Cung cấp dữ liệu tổng hợp gọn nhẹ cho dashboard quản trị.
--   Spring Boot gọi các procedure này qua DashboardRepositoryImpl bằng native query của EntityManager.
--
-- Quy tắc ngày:
--   p_from_date được tính bao gồm.
--   p_to_date được tính bao gồm bằng cách đổi điều kiện thành: created_at < p_to_date + 1 ngày.
--
-- Quy tắc chi nhánh:
--   p_branch_id = NULL nghĩa là xem dashboard toàn hệ thống.
--   p_branch_id có giá trị nghĩa là dashboard chỉ lấy dữ liệu của một chi nhánh.
--
-- Quy tắc doanh thu:
--   Doanh thu và giá trị đơn trung bình chỉ tính từ đơn COMPLETED.
--   Đơn CANCELLED chỉ được đếm riêng, không cộng vào doanh thu.

DROP PROCEDURE IF EXISTS sp_dashboard_overview;
DROP PROCEDURE IF EXISTS sp_dashboard_revenue_trend;
DROP PROCEDURE IF EXISTS sp_dashboard_order_status;
DROP PROCEDURE IF EXISTS sp_dashboard_top_products;
DROP PROCEDURE IF EXISTS sp_dashboard_branch_performance;

CREATE PROCEDURE sp_dashboard_overview(
    IN p_from_date DATE,
    IN p_to_date DATE,
    IN p_branch_id CHAR(36)
)
BEGIN
    -- Trả dữ liệu cho các thẻ KPI:
    --   totalRevenue: tổng total_amount của đơn đã hoàn tất.
    --   completedOrders: số lượng đơn đã hoàn tất.
    --   cancelledOrders: số lượng đơn đã hủy.
    --   averageOrderValue: doanh thu đã hoàn tất chia cho số đơn đã hoàn tất.
    --   newCustomers: khách có đơn đầu tiên nằm trong khoảng ngày đã chọn.
    SELECT
        COALESCE(SUM(CASE WHEN o.status = 'COMPLETED' THEN o.total_amount ELSE 0 END), 0) AS totalRevenue,
        COALESCE(SUM(CASE WHEN o.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completedOrders,
        COALESCE(SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelledOrders,
        COALESCE(
            SUM(CASE WHEN o.status = 'COMPLETED' THEN o.total_amount ELSE 0 END)
                / NULLIF(SUM(CASE WHEN o.status = 'COMPLETED' THEN 1 ELSE 0 END), 0),
            0
        ) AS averageOrderValue,
        COALESCE((
            SELECT COUNT(DISTINCT first_orders.customer_id)
            FROM (
                SELECT customer_id, MIN(created_at) AS first_order_at
                FROM od_order
                WHERE customer_id IS NOT NULL
                    AND (p_branch_id IS NULL OR branch_id = p_branch_id)
                GROUP BY customer_id
            ) first_orders
            WHERE first_orders.first_order_at >= p_from_date
                AND first_orders.first_order_at < DATE_ADD(p_to_date, INTERVAL 1 DAY)
        ), 0) AS newCustomers
    FROM od_order o
    WHERE o.created_at >= p_from_date
        AND o.created_at < DATE_ADD(p_to_date, INTERVAL 1 DAY)
        AND (p_branch_id IS NULL OR o.branch_id = p_branch_id);
END;

CREATE PROCEDURE sp_dashboard_revenue_trend(
    IN p_from_date DATE,
    IN p_to_date DATE,
    IN p_branch_id CHAR(36)
)
BEGIN
    -- Trả một dòng cho mỗi ngày để vẽ biểu đồ đường.
    -- Chỉ tính đơn COMPLETED vì biểu đồ này thể hiện doanh thu thực nhận.
    SELECT
        DATE(o.created_at) AS revenueDate,
        COALESCE(SUM(o.total_amount), 0) AS revenue,
        COUNT(*) AS orders,
        COALESCE(SUM(o.total_amount) / NULLIF(COUNT(*), 0), 0) AS averageOrderValue
    FROM od_order o
    WHERE o.status = 'COMPLETED'
        AND o.created_at >= p_from_date
        AND o.created_at < DATE_ADD(p_to_date, INTERVAL 1 DAY)
        AND (p_branch_id IS NULL OR o.branch_id = p_branch_id)
    GROUP BY DATE(o.created_at)
    ORDER BY revenueDate ASC;
END;

CREATE PROCEDURE sp_dashboard_order_status(
    IN p_from_date DATE,
    IN p_to_date DATE,
    IN p_branch_id CHAR(36)
)
BEGIN
    -- Trả số lượng đơn theo từng trạng thái xử lý.
    -- Dùng cho biểu đồ phân bố trạng thái và theo dõi tồn đọng vận hành.
    SELECT
        o.status AS orderStatus,
        COUNT(*) AS orderCount
    FROM od_order o
    WHERE o.created_at >= p_from_date
        AND o.created_at < DATE_ADD(p_to_date, INTERVAL 1 DAY)
        AND (p_branch_id IS NULL OR o.branch_id = p_branch_id)
    GROUP BY o.status
    ORDER BY FIELD(o.status, 'PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'DELIVERING', 'COMPLETED', 'CANCELLED', 'REJECTED');
END;

CREATE PROCEDURE sp_dashboard_top_products(
    IN p_from_date DATE,
    IN p_to_date DATE,
    IN p_branch_id CHAR(36),
    IN p_limit INT
)
BEGIN
    -- Trả danh sách sản phẩm bán chạy theo số lượng, sau đó theo doanh thu.
    -- Dùng dữ liệu snapshot trong od_order_item để tránh join bảng sản phẩm khi không cần thiết.
    SELECT
        oi.product_id AS productId,
        oi.product_name AS productName,
        COALESCE(SUM(oi.quantity), 0) AS quantitySold,
        COALESCE(SUM(oi.total_price), 0) AS revenue
    FROM od_order_item oi
    JOIN od_order o ON o.id = oi.order_id
    WHERE o.status = 'COMPLETED'
        AND o.created_at >= p_from_date
        AND o.created_at < DATE_ADD(p_to_date, INTERVAL 1 DAY)
        AND (p_branch_id IS NULL OR o.branch_id = p_branch_id)
    GROUP BY oi.product_id, oi.product_name
    ORDER BY quantitySold DESC, revenue DESC
    LIMIT p_limit;
END;

CREATE PROCEDURE sp_dashboard_branch_performance(
    IN p_from_date DATE,
    IN p_to_date DATE
)
BEGIN
    -- Trả chỉ số so sánh hiệu suất giữa các chi nhánh.
    -- Bao gồm cả chi nhánh chưa có đơn nhờ LEFT JOIN, giúp dashboard hiển thị giá trị 0.
    SELECT
        b.id AS branchId,
        b.name AS branchName,
        COALESCE(SUM(CASE WHEN o.status = 'COMPLETED' THEN o.total_amount ELSE 0 END), 0) AS revenue,
        COALESCE(SUM(CASE WHEN o.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completedOrders,
        COALESCE(SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelledOrders,
        COALESCE(
            SUM(CASE WHEN o.status = 'COMPLETED' THEN o.total_amount ELSE 0 END)
                / NULLIF(SUM(CASE WHEN o.status = 'COMPLETED' THEN 1 ELSE 0 END), 0),
            0
        ) AS averageOrderValue
    FROM ce_branch b
    LEFT JOIN od_order o ON o.branch_id = b.id
        AND o.created_at >= p_from_date
        AND o.created_at < DATE_ADD(p_to_date, INTERVAL 1 DAY)
    GROUP BY b.id, b.name
    ORDER BY revenue DESC, completedOrders DESC, b.name ASC;
END;

-- Index hỗ trợ lọc và join cho các truy vấn tổng hợp dashboard.
CREATE INDEX idx_od_order_status_created_branch ON od_order(status, created_at, branch_id);
CREATE INDEX idx_od_item_order_product ON od_order_item(order_id, product_id);
CREATE INDEX idx_cu_customer_created ON cu_customer_profile(created_at);
