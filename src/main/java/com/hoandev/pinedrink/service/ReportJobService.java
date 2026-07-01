package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Report.CreateReportJobRequest;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportJobResponse;
import org.springframework.core.io.Resource;

/**
 * Service ứng dụng cho các job xuất báo cáo.
 * <p>
 * Service này quản lý luồng phục vụ API: tạo job, kiểm tra quyền sở hữu job,
 * trả về trạng thái job và tải file kết quả để người dùng download.
 */
public interface ReportJobService {

    /**
     * Tạo một job xuất báo cáo mới cho người dùng đã xác thực.
     * <p>
     * Job sau khi tạo sẽ được đưa vào hàng đợi xử lý nền và thường bắt đầu với
     * trạng thái {@code PENDING}.
     *
     * @param request loại báo cáo, định dạng file và bộ lọc được yêu cầu
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @return thông tin siêu dữ liệu của job vừa tạo
     */
    ReportJobResponse createJob(CreateReportJobRequest request, String requestedById);

    /**
     * Trả về job báo cáo thuộc sở hữu của người dùng đã xác thực.
     *
     * @param jobId mã định danh của job báo cáo
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @return thông tin siêu dữ liệu hiện tại của job báo cáo
     */
    ReportJobResponse getJob(String jobId, String requestedById);

    /**
     * Tải file đã sinh cho một job báo cáo đã hoàn tất.
     *
     * @param jobId mã định danh của job báo cáo
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @return tài nguyên báo cáo đã sinh, sẵn sàng trả về qua HTTP download
     */
    Resource download(String jobId, String requestedById);
}
