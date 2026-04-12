package com.huang.common.guard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedTaskLock {

    String taskCode();

    long ttlSec() default 300L;

    boolean failOpen() default false;
}
