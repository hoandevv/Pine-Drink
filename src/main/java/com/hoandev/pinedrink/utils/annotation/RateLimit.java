package com.hoandev.pinedrink.utils.annotation;

import com.hoandev.pinedrink.entity.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key();
    long limit() default 10;
    long duration() default 60;
    RateLimitType type() default RateLimitType.IP;
    boolean failClosed() default false;
}
