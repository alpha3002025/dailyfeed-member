package click.dailyfeed.member.config.web.annotation;

import click.dailyfeed.member.config.web.validator.UniqueHandleValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueHandleValidator.class)
public @interface UniqueHandle {
    String message() default "이미 사용 중인 핸들입니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
