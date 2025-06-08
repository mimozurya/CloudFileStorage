package com.cloud.storage.annotation;

import com.cloud.storage.validator.DirectoryNotExistsValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DirectoryNotExistsValidator.class)
public @interface DirectoryNotExists {
    String message() default "Directory already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
