package com.hoandev.pinedrink.service;

/**
 * Service chịu trách nhiệm thực thi các job xuất báo cáo.
 * <p>
 * Service này chứa luồng nghiệp vụ để tải yêu cầu xuất báo cáo, sinh file,
 * lưu file và cập nhật trạng thái job. Service được tách độc lập khỏi
 * RabbitMQ để tầng nhắn tin chỉ đóng vai trò adapter vận chuyển thông điệp.
 */
public interface ReportExportService {

    /**
     * Thực thi một job xuất báo cáo đang ở trạng thái chờ xử lý.
     * <p>
     * Implementation nên có tính idempotent với các job không còn ở trạng thái
     * {@code PENDING}: nếu job không tồn tại hoặc đã chuyển sang trạng thái khác,
     * lời gọi nên kết thúc mà không sinh thêm file mới.
     *
     * @param jobId mã định danh của job xuất báo cáo
     */
    void export(String jobId);
}
