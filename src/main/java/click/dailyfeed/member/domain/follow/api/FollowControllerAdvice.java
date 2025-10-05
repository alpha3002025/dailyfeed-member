package click.dailyfeed.member.domain.follow.api;

import click.dailyfeed.code.domain.member.follow.exception.FollowException;
import click.dailyfeed.code.domain.member.member.exception.MemberException;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "click.dailyfeed.member.domain.follow.api")
public class FollowControllerAdvice {

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

    @ExceptionHandler(value = FollowException.class)
    public DailyfeedErrorResponse handleFollowException(
            FollowException e,
            HttpServletRequest request
    ){
        return DailyfeedErrorResponse.of(
                400,
                ResponseSuccessCode.FAIL,
                e.getFollowExceptionCode().getMessage(),
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

    @ExceptionHandler(value = RuntimeException.class)
    public DailyfeedErrorResponse handleException(
            RuntimeException e,
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
