package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "rp_export_request")
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "requested_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "createdBy", column = @Column(name = "requested_by", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
})
public class ExportRequest extends BaseEntity {

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "file_format", nullable = false)
    private String fileFormat = "XLSX";

    @Column(columnDefinition = "json")
    private String filters;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private Account requestedBy;
}
