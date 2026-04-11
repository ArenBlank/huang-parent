package com.huang.common.guard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RateLimits.class)
public @interface RateLimit {
    String prefix();

    String key();

    long maxRequests();

    long windowSec();

    String message() default "请求过于频繁，请稍后再试";

    boolean failOpen() default true;
}
