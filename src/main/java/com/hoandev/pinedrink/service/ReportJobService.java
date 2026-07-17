package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Report.CreateReportJobRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportJobResponse;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportJobStatsResponse;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportOptionsResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

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
     * Trả về lịch sử xuất báo cáo của người dùng đã xác thực.
     *
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @param pageable thông tin phân trang và sắp xếp
     * @param fromDate ngày bắt đầu lọc theo thời gian tạo job
     * @param toDate ngày kết thúc lọc theo thời gian tạo job
     * @return danh sách job báo cáo đã phân trang
     */
    PageResponse<ReportJobResponse> getJobs(String requestedById, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    ReportJobStatsResponse getStats(String requestedById);

    /**
     * Trả về dữ liệu khởi tạo cho trang báo cáo.
     *
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @return danh mục active dùng cho filter và thống kê job báo cáo
     */
    ReportOptionsResponse getOptions(String requestedById);

    /**
     * Tải file đã sinh cho một job báo cáo đã hoàn tất.
     *
     * @param jobId mã định danh của job báo cáo
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @return tài nguyên báo cáo đã sinh, sẵn sàng trả về qua HTTP download
     */
    Resource download(String jobId, String requestedById);
}
