package com.hoandev.pinedrink.queue.listener;

import com.hoandev.pinedrink.queue.event.report.ReportExportRequestedEvent;
import com.hoandev.pinedrink.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Bộ lắng nghe RabbitMQ cho các job xuất báo cáo.
 * <p>
 * Lớp này là adapter nhắn tin cho chức năng xuất báo cáo. Nó nhận các thông điệp
 * {@link ReportExportRequestedEvent} từ background-job queue và ủy quyền toàn bộ
 * nghiệp vụ xử lý cho {@link ReportExportService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportExportListener {

    private final ReportExportService reportExportService;

    /**
     * Consume yêu cầu xuất báo cáo từ background-job queue đã cấu hình.
     * <p>
     * Concurrency và prefetch của listener được đọc từ
     * {@code app.rabbitmq.background-job} để có thể tinh chỉnh việc xuất báo cáo
     * mà không cần sửa code.
     *
     * @param event thông điệp mô tả job báo cáo cần được xuất
     */
    @RabbitListener(
            queues = "${app.rabbitmq.report.queue}",
            containerFactory = "reportListenerContainerFactory"
    )
    public void handleReportExportRequested(ReportExportRequestedEvent event) {
        log.info("Received report export event: eventId={}, jobId={}", event.eventId(), event.jobId());
        try {
            reportExportService.export(event.jobId());
        } catch (Exception e) {
            log.error("Report export listener handled failure without message retry: eventId={}, jobId={}",
                    event.eventId(), event.jobId(), e);
        }
    }
}
