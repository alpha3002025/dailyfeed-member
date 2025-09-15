package click.dailyfeed.member.config.web.validator;

import click.dailyfeed.code.domain.member.member.predicate.HandleExistsPredicate;
import click.dailyfeed.member.config.web.annotation.UniqueHandle;
import click.dailyfeed.member.domain.member.redis.MemberRedisService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UniqueHandleValidator implements ConstraintValidator<UniqueHandle, String> {
    @Autowired
    private MemberRedisService memberRedisService;

    @Override
    public boolean isValid(String handle, ConstraintValidatorContext constraintValidatorContext) {
        if (handle == null || handle.trim().isEmpty()) {
            return true; // @NotBlank에서 처리
        }

        HandleExistsPredicate check = memberRedisService.isHandleExists(handle);
        if (check == HandleExistsPredicate.NOT_EXISTS) {
            return true;
        }
        return false;
    }
}
