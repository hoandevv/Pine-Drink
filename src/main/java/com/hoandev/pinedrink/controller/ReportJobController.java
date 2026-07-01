package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Report.CreateReportJobRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportJobResponse;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.service.ReportJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/jobs")
@RequiredArgsConstructor
public class ReportJobController {

    private final ReportJobService reportJobService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ReportJobResponse>> createJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReportJobRequest request) {
        ReportJobResponse response = reportJobService.createJob(request, principal.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(BaseResponse.success(response, "Report job created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ReportJobResponse>> getJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.success(reportJobService.getJob(id, principal.getId())));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
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
