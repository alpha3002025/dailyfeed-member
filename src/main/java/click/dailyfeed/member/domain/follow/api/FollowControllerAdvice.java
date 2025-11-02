package click.dailyfeed.member.domain.follow.api;

import click.dailyfeed.code.domain.member.follow.exception.FollowException;
import click.dailyfeed.code.domain.member.key.exception.JwtKeyException;
import click.dailyfeed.code.domain.member.member.code.MemberHeaderCode;
import click.dailyfeed.code.domain.member.member.exception.MemberException;
import click.dailyfeed.code.domain.member.token.exception.KeyRefreshErrorException;
import click.dailyfeed.code.domain.member.token.exception.TokenRefreshNeededException;
import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "click.dailyfeed.member.domain.follow.api")
public class FollowControllerAdvice {

    @ExceptionHandler(value = MemberException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
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

    @ExceptionHandler(JwtKeyException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public DailyfeedErrorResponse handleJwtKeyException(JwtKeyException e, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(MemberHeaderCode.X_RELOGIN_REQUIRED.getHeaderKey(), "true");
        return DailyfeedErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ResponseSuccessCode.FAIL,
                e.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public DailyfeedErrorResponse handleInvalidTokenException(InvalidTokenException e, HttpServletRequest request, HttpServletResponse response) {
        log.error("Invalid token error: {}", e.getMessage());
        response.setHeader(MemberHeaderCode.X_RELOGIN_REQUIRED.getHeaderKey(), "true");
        return DailyfeedErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ResponseSuccessCode.FAIL,
                e.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(KeyRefreshErrorException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public DailyfeedErrorResponse handleKeyRefreshErrorException(KeyRefreshErrorException e, HttpServletRequest request, HttpServletResponse response) {
        return DailyfeedErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ResponseSuccessCode.FAIL,
                e.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(TokenRefreshNeededException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public DailyfeedErrorResponse handleTokenRefreshNeededException(TokenRefreshNeededException e, HttpServletRequest request) {
        return DailyfeedErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ResponseSuccessCode.FAIL,
                e.getTokenExceptionCode().getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(value = FollowException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
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
    @ResponseStatus(HttpStatus.BAD_REQUEST)
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
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public DailyfeedErrorResponse handleRuntimeException(
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
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
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
