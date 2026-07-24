package com.hoandev.pinedrink.queue.listener;

import com.hoandev.pinedrink.queue.event.report.ReportExportRequestedEvent;
import com.hoandev.pinedrink.service.ReportExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Bộ lắng nghe RabbitMQ cho các job xuất báo cáo.
 * <p>
 * Lớp này là adapter nhắn tin cho chức năng xuất báo cáo. Nó nhận các thông điệp
 * {@link ReportExportRequestedEvent} từ report queue và ủy quyền toàn bộ
 * nghiệp vụ xử lý cho {@link ReportExportService}.
 */
@Slf4j
@Component
public class ReportExportListener {

    private final ReportExportService reportExportService;

    public ReportExportListener(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    /**
     * Consume yêu cầu xuất báo cáo từ report queue đã cấu hình.
     * <p>
     * @param event thông điệp mô tả job báo cáo cần được xuất
     */
    @RabbitListener(queues = "${app.rabbitmq.report.queue}")
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
