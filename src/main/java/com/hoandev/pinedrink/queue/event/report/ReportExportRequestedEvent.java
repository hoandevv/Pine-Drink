package com.hoandev.pinedrink.queue.event.report;

import com.hoandev.pinedrink.queue.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event được publish khi một job xuất báo cáo đã sẵn sàng để xử lý.
 * <p>
 * Event chỉ mang mã định danh của job. Consumer phải tải lại job từ cơ sở dữ liệu
 * trước khi xử lý để thông điệp RabbitMQ gọn nhẹ và luồng xử lý xuất báo cáo luôn
 * dùng trạng thái job mới nhất đã được lưu.
 *
 * @param eventId mã định danh duy nhất của event, dùng cho tracing
 * @param eventType mã phân loại ổn định của loại event này
 * @param occurredAt thời điểm event được tạo
 * @param source service logic đã tạo event
 * @param jobId mã định danh của job xuất báo cáo đã được lưu
 */
public record ReportExportRequestedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String source,
        String jobId
) implements DomainEvent {

    /**
     * Tạo event xuất báo cáo với siêu dữ liệu tiêu chuẩn.
     *
     * @param jobId mã định danh của job xuất báo cáo cần xử lý
     * @return event xuất báo cáo mới
     */
    public static ReportExportRequestedEvent of(String jobId) {
        return new ReportExportRequestedEvent(
                UUID.randomUUID().toString(),
                "REPORT_EXPORT_REQUESTED",
                Instant.now(),
                "report-job-service",
                jobId
        );
    }
}
