package click.dailyfeed.member.domain.authentication.api;

import click.dailyfeed.code.domain.member.member.exception.MemberException;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "click.dailyfeed.member.domain.authentication.api")
public class AuthenticationControllerAdvice {

    @ExceptionHandler(value = MemberException.class)
    public DailyfeedErrorResponse handleMemberException(
            MemberException e,
            HttpServletRequest request) {

        return DailyfeedErrorResponse.of(
                e.getMemberExceptionCode().getCode(),
                ResponseSuccessCode.FAIL,
                e.getMemberExceptionCode().getMessage(),
                request.getRequestURI()
        );
    }

    // validation
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public DailyfeedErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ){
        StringBuilder errors = new StringBuilder();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            if (errors.length() > 0) {
                errors.append(", ");
            }
            errors.append(error.getDefaultMessage());
        });

        return DailyfeedErrorResponse.of(
                400,
                ResponseSuccessCode.FAIL,
                errors.toString(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    public DailyfeedErrorResponse handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ){
        StringBuilder errors = new StringBuilder();
        e.getConstraintViolations().forEach(violation -> {
            if (errors.length() > 0) {
                errors.append(", ");
            }
            errors.append(violation.getMessage());
        });

        return DailyfeedErrorResponse.of(
                400,
                ResponseSuccessCode.FAIL,
                errors.toString(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(value = Exception.class)
    public DailyfeedErrorResponse handleException(
            Exception e,
            HttpServletRequest request
    ){
        e.printStackTrace();
        log.error(e.getMessage(), e);
        return DailyfeedErrorResponse.of(
                500,
                ResponseSuccessCode.FAIL,
                "서버 내부 오류가 발생했습니다",
                request.getRequestURI()
        );
    }

}
