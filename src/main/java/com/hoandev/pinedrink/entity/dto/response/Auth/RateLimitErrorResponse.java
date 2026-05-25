package com.hoandev.pinedrink.entity.dto.response.Auth;

import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RateLimitErrorResponse extends BaseResponse<Object> {

    /**
     * Số giây còn lại trước khi có thể thử lại
     */
    private Long retryAfter;

    /**
     * Số request tối đa được phép
     */
    private Integer limit;

    /**
     * Số request đã sử dụng
     */
    private Integer remaining;

    /**
     * Timestamp khi rate limit sẽ được reset (Unix timestamp)
     */
    private Long resetTime;

    public RateLimitErrorResponse(String code, String message, Long retryAfter, Integer limit, Integer remaining, Long resetTime) {
        super(false, code, message, LocalDateTime.now(), null, null);
        this.retryAfter = retryAfter;
        this.limit = limit;
        this.remaining = remaining;
        this.resetTime = resetTime;
    }
}