package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.configuration.RabbitMqProperties;
import com.hoandev.pinedrink.configuration.ReportStorageProperties;
import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.ExportRequest;
import com.hoandev.pinedrink.entity.dto.request.Report.CreateReportJobRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportJobResponse;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportJobStatsResponse;
import com.hoandev.pinedrink.entity.enums.ExportRequestStatus;
import com.hoandev.pinedrink.entity.enums.ReportFileFormat;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.ReportJobMapper;
import com.hoandev.pinedrink.queue.event.report.ReportExportRequestedEvent;
import com.hoandev.pinedrink.queue.publisher.EventPublisher;
import com.hoandev.pinedrink.repository.AccountRepository;
import com.hoandev.pinedrink.repository.BranchRepository;
import com.hoandev.pinedrink.repository.ExportRequestRepository;
import com.hoandev.pinedrink.service.ReportJobService;
import com.hoandev.pinedrink.service.ReportStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportJobServiceImpl implements ReportJobService {
    private static final long MAX_REPORT_RANGE_DAYS = 366;

    private final ExportRequestRepository exportRequestRepository;
    private final AccountRepository accountRepository;
    private final BranchRepository branchRepository;
    private final EventPublisher eventPublisher;
    private final RabbitMqProperties rabbitMqProperties;
    private final ReportStorageProperties reportStorageProperties;
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
            throw new BaseException(ErrorCode.REPORT_001);
        }

        Account requestedBy = accountRepository.findById(requestedById)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));

        ExportRequest job = new ExportRequest();
        job.setReportType(request.getReportType().name());
        job.setFileFormat(request.getFileFormat().name());
        job.setFilters(request.getFilters());
        job.setStatus(ExportRequestStatus.PENDING.name());
        job.setRequestedBy(requestedBy);
        if (request.getBranchId() != null && !request.getBranchId().isBlank()) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new BaseException(ErrorCode.COM_005, "Branch not found"));
            job.setBranch(branch);
        }

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
    @Transactional
    public ReportJobResponse getJob(String jobId, String requestedById) {
        ExportRequest job = getOwnedJob(jobId, requestedById);
        markStaleRunningJobAsFailed(job);
        return reportJobMapper.toResponse(job);
    }

    /**
     * Trả về lịch sử xuất báo cáo của người dùng đã xác thực.
     *
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @param pageable thông tin phân trang và sắp xếp
     * @return danh sách job báo cáo đã phân trang
     */
    @Override
    @Transactional
    public PageResponse<ReportJobResponse> getJobs(
            String requestedById,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        validateDateRange(fromDate, toDate);
        LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
        Page<ExportRequest> jobs = exportRequestRepository.findByRequestedByIdAndCreatedAtRange(
                requestedById, fromDateTime, toDateTime, pageable);
        jobs.getContent().forEach(this::markStaleRunningJobAsFailed);
        return PageResponse.from(jobs, jobs.getContent().stream()
                .map(reportJobMapper::toResponse)
                .toList());
    }

    /**
     * Trả về thống kê thật trên toàn bộ job báo cáo của người dùng.
     * <p>
     * Job RUNNING quá timeout được tính là failed để khớp logic kiểm tra stale job.
     *
     * @param requestedById id tài khoản của người dùng đã xác thực
     * @return tổng số job và số lượng theo trạng thái chính
     */
    @Override
    @Transactional(readOnly = true)
    public ReportJobStatsResponse getStats(String requestedById) {
        LocalDateTime staleRunningBefore = LocalDateTime.now().minus(reportStorageProperties.getRunningTimeout());
        long staleRunning = exportRequestRepository.countStaleByStatus(
                requestedById,
                ExportRequestStatus.RUNNING.name(),
                staleRunningBefore
        );

        long running = List.of(ExportRequestStatus.PENDING, ExportRequestStatus.RUNNING).stream()
                .mapToLong(status -> exportRequestRepository.countByStatus(requestedById, status.name()))
                .sum() - staleRunning;

        return ReportJobStatsResponse.builder()
                .total(exportRequestRepository.countByRequestedById(requestedById))
                .completed(exportRequestRepository.countByStatus(requestedById, ExportRequestStatus.DONE.name()))
                .running(Math.max(running, 0))
                .failed(exportRequestRepository.countByStatus(requestedById, ExportRequestStatus.FAILED.name())
                        + staleRunning)
                .build();
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
            throw new BaseException(ErrorCode.REPORT_002);
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
                .orElseThrow(() -> new BaseException(ErrorCode.REPORT_003));
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
            var report = rabbitMqProperties.report();
            try {
                eventPublisher.publish(
                        report.exchange(),
                        report.routingKey(),
                        ReportExportRequestedEvent.of(jobId)
                );
            } catch (Exception e) {
                log.error("Failed to publish report export event: jobId={}", jobId, e);
                markPendingJobAsFailed(jobId, e);
            }
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
    /**
     * Đánh dấu job fail khi publish event thất bại,
     *
     * chỉ áp dụng cho job đang ở trạng thái PENDING.
     */
    private void markPendingJobAsFailed(String jobId, Exception e) {
        exportRequestRepository.findById(jobId)
                .filter(job -> ExportRequestStatus.PENDING.name().equals(job.getStatus()))
                .ifPresent(job -> {
                    job.setStatus(ExportRequestStatus.FAILED.name());
                    job.setErrorMessage(limitError("Failed to queue report export: " + e.getMessage()));
                    job.setCompletedAt(LocalDateTime.now());
                    exportRequestRepository.save(job);
                });
    }
    /**
     * Đánh dấu job đang chạy nhưng đã hết thời gian chờ -> fail
     *
     * @param job entity của job
     */
    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BaseException(ErrorCode.REPORT_004);
        }
        if (fromDate != null && toDate != null && java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) > MAX_REPORT_RANGE_DAYS) {
            throw new BaseException(ErrorCode.REPORT_007);
        }
    }

    private void markStaleRunningJobAsFailed(ExportRequest job) {
        if (!ExportRequestStatus.RUNNING.name().equals(job.getStatus()) || job.getStartedAt() == null) {
            return;
        }
        if (job.getStartedAt().plus(reportStorageProperties.getRunningTimeout()).isAfter(LocalDateTime.now())) {
            return;
        }

        job.setStatus(ExportRequestStatus.FAILED.name());
        job.setErrorMessage("Report export timed out");
        job.setCompletedAt(LocalDateTime.now());
        exportRequestRepository.save(job);
    }

    private String limitError(String message) {
        if (message == null || message.isBlank()) {
            return "Failed to queue report export";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

}
