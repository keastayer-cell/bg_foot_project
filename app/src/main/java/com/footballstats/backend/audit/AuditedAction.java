package com.footballstats.backend.audit;
import java.lang.annotation.*;
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditedAction {
    String entity();
    String idParam();
    String action();
    String actorParam() default "actorUserId";
}
