package click.dailyfeed.member.domain.member.api;

import click.dailyfeed.code.domain.member.member.exception.MemberException;
import click.dailyfeed.code.global.jwt.exception.JwtException;
import click.dailyfeed.code.global.web.response.DailyfeedErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
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
                e.getMemberExceptionCode().getMessage(),
                e.getMemberExceptionCode().getReason(),
                e.getMemberExceptionCode().getCode(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public DailyfeedErrorResponse handleJwtException(JwtException e, HttpServletRequest request) {
        return DailyfeedErrorResponse.of(
                e.getJwtExceptionCode().getMessage(),
                e.getJwtExceptionCode().getReason(),
                e.getJwtExceptionCode().getCode(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public DailyfeedErrorResponse handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("Unexpected runtime exception occurred", e);

        return DailyfeedErrorResponse.of(
                "서버 내부 오류가 발생했습니다.",
                "INTERNAL_SERVER_ERROR",
                500,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public DailyfeedErrorResponse handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected runtime exception occurred", e);

        return DailyfeedErrorResponse.of(
                "서버 내부 오류가 발생했습니다.",
                "INTERNAL_SERVER_ERROR",
                500,
                request.getRequestURI()
        );
    }
}
