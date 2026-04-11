package com.huang.common.guard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(IdempotentSubmits.class)
public @interface IdempotentSubmit {
    String prefix();

    String key();

    long ttlSec();

    String message() default "请勿重复提交";

    boolean failOpen() default true;
}
