package com.hoandev.pinedrink.entity.dto.request.Branch;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchHoursRequest {

    @Min(value = 1, message = "Day of week must be from 1 to 7")
    @Max(value = 7, message = "Day of week must be from 1 to 7")
    private int dayOfWeek;

    @NotNull(message = "Open time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime;

    @NotNull(message = "Close time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime;

    @Builder.Default
    private boolean closed = false;
}
