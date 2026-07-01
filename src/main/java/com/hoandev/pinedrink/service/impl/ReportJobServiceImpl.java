package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.configuration.RabbitMqProperties;
import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.ExportRequest;
import com.hoandev.pinedrink.entity.dto.request.Report.CreateReportJobRequest;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportJobResponse;
import com.hoandev.pinedrink.entity.enums.ExportRequestStatus;
import com.hoandev.pinedrink.entity.enums.ReportFileFormat;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.ReportJobMapper;
import com.hoandev.pinedrink.queue.event.report.ReportExportRequestedEvent;
import com.hoandev.pinedrink.queue.publisher.EventPublisher;
import com.hoandev.pinedrink.repository.AccountRepository;
import com.hoandev.pinedrink.repository.ExportRequestRepository;
import com.hoandev.pinedrink.service.ReportJobService;
import com.hoandev.pinedrink.service.ReportStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class ReportJobServiceImpl implements ReportJobService {

    private final ExportRequestRepository exportRequestRepository;
    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;
    private final RabbitMqProperties rabbitMqProperties;
    private final ReportStorageService reportStorageService;
    private final ReportJobMapper reportJobMapper;

    /**
     * Tạo job xuất báo cáo và publish thông điệp RabbitMQ để xử lý nền.
     * <p>
     * HTTP request chỉ lưu job và đưa công việc vào queue. Việc sinh PDF thực tế
     * sẽ được xử lý sau bởi {@code ReportExportListener}.
     *
     * @param request loại báo cáo, định dạng file và filter JSON tùy chọn
     * @param requestedById id tài khoản của người dùng đã xác thực đang tạo job
     * @return siêu dữ liệu của job đã lưu với trạng thái {@code PENDING}
     */
    @Override
    @Transactional
    public ReportJobResponse createJob(CreateReportJobRequest request, String requestedById) {
        if (request.getFileFormat() != ReportFileFormat.PDF) {
            throw new BaseException(ErrorCode.COM_004, "Only PDF report export is supported now");
        }

        Account requestedBy = accountRepository.findById(requestedById)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));

        ExportRequest job = new ExportRequest();
        job.setReportType(request.getReportType().name());
        job.setFileFormat(request.getFileFormat().name());
        job.setFilters(request.getFilters());
        job.setStatus(ExportRequestStatus.PENDING.name());
        job.setRequestedBy(requestedBy);

        ExportRequest saved = exportRequestRepository.save(job);
        publishReportExportRequestedAfterCommit(saved.getId());
        return reportJobMapper.toResponse(saved);
    }

    /**
     * Trả về job báo cáo thuộc sở hữu của người dùng đã xác thực.
     *
     * @param jobId mã định danh của job báo cáo
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @return siêu dữ liệu của job và trạng thái xử lý hiện tại
     */
    @Override
    @Transactional(readOnly = true)
    public ReportJobResponse getJob(String jobId, String requestedById) {
        return reportJobMapper.toResponse(getOwnedJob(jobId, requestedById));
    }

    /**
     * Tải file báo cáo đã hoàn tất thuộc sở hữu của người dùng đã xác thực.
     *
     * @param jobId mã định danh của job báo cáo
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @return tài nguyên Spring đại diện cho file báo cáo đã sinh
     */
    @Override
    @Transactional(readOnly = true)
    public Resource download(String jobId, String requestedById) {
        ExportRequest job = getOwnedJob(jobId, requestedById);
        if (!ExportRequestStatus.DONE.name().equals(job.getStatus()) || job.getFileUrl() == null) {
            throw new BaseException(ErrorCode.COM_004, "Report file is not ready");
        }
        return reportStorageService.load(job.getFileUrl());
    }

    /**
     * Tìm job và kiểm tra job đó có thuộc về người dùng đã xác thực hay không.
     *
     * @param jobId mã định danh của job báo cáo
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @return entity export request thuộc sở hữu của người dùng
     */
    private ExportRequest getOwnedJob(String jobId, String requestedById) {
        ExportRequest job = exportRequestRepository.findById(jobId)
                .orElseThrow(() -> new BaseException(ErrorCode.COM_005, "Report job not found"));
        if (job.getRequestedBy() == null || !requestedById.equals(job.getRequestedBy().getId())) {
            throw new BaseException(ErrorCode.AUTH_007);
        }
        return job;
    }

    /**
     * Publish event xuất báo cáo chỉ sau khi transaction cơ sở dữ liệu đã commit.
     * <p>
     * Việc trì hoãn publish giúp tránh trường hợp RabbitMQ consumer nhận job id
     * trước khi bản ghi {@link ExportRequest} có thể đọc được trong cơ sở dữ liệu.
     *
     * @param jobId mã định danh của export request vừa tạo
     */
    private void publishReportExportRequestedAfterCommit(String jobId) {
        Runnable publish = () -> {
            var backgroundJob = rabbitMqProperties.backgroundJob();
            eventPublisher.publish(
                    backgroundJob.exchange(),
                    backgroundJob.routingKey(),
                    ReportExportRequestedEvent.of(jobId)
            );
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
            return;
        }

        publish.run();
    }

}
