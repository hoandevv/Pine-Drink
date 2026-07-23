package com.hoandev.pinedrink.entity.dto.request.Branch;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBranchHoursRequest {

    @Min(value = 1, message = "Day of week must be from 1 to 7")
    @Max(value = 7, message = "Day of week must be from 1 to 7")
    private Integer dayOfWeek;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime;

    private Boolean closed;
}
