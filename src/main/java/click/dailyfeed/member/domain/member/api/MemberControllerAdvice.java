package click.dailyfeed.member.domain.member.api;

import click.dailyfeed.code.domain.member.key.exception.JwtKeyException;
import click.dailyfeed.code.domain.member.member.code.MemberHeaderCode;
import click.dailyfeed.code.domain.member.member.exception.MemberException;
import click.dailyfeed.code.domain.member.token.exception.KeyRefreshErrorException;
import click.dailyfeed.code.domain.member.token.exception.TokenRefreshNeededException;
import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.code.global.jwt.exception.JwtException;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "click.dailyfeed.member.domain.member.api")
public class MemberControllerAdvice {

    @ExceptionHandler(MemberException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DailyfeedErrorResponse handleMemberException(MemberException e, HttpServletRequest request) {
        return DailyfeedErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
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

    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public DailyfeedErrorResponse handleJwtException(JwtException e, HttpServletRequest request) {
        return DailyfeedErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ResponseSuccessCode.FAIL,
                e.getJwtExceptionCode().getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public DailyfeedErrorResponse handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        return DailyfeedErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ResponseSuccessCode.FAIL,
                "서버 내부 오류가 발생했습니다.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public DailyfeedErrorResponse handleException(Exception e, HttpServletRequest request) {
        return DailyfeedErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ResponseSuccessCode.FAIL,
                "서버 내부 오류가 발생했습니다.",
                request.getRequestURI()
        );
    }
}
