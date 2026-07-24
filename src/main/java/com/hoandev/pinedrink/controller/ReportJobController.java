package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Report.CreateReportJobRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportJobResponse;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportJobStatsResponse;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportOptionsResponse;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.service.ReportJobService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports/jobs")
public class ReportJobController {

    private final ReportJobService reportJobService;

    public ReportJobController(ReportJobService reportJobService) {
        this.reportJobService = reportJobService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_REPORT_CREATE')")
    public ResponseEntity<BaseResponse<ReportJobResponse>> createJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReportJobRequest request) {
        ReportJobResponse response = reportJobService.createJob(request, principal.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(BaseResponse.success(response, "Report job created successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<BaseResponse<ReportJobStatsResponse>> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(reportJobService.getStats(principal.getId()),
                "Report job stats retrieved successfully"));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<BaseResponse<ReportOptionsResponse>> getOptions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(reportJobService.getOptions(principal.getId()),
                "Report options retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<BaseResponse<ReportJobResponse>> getJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.success(reportJobService.getJob(id, principal.getId())));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<ReportJobResponse>>> getJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.success(reportJobService.getJobs(principal.getId(), fromDate, toDate, pageable),
                "Report jobs retrieved successfully"));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        Resource resource = reportJobService.download(id, principal.getId());
        String filename = resource.getFilename() == null ? "report.pdf" : resource.getFilename();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(resource);
    }
}
