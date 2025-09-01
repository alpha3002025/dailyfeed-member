package click.dailyfeed.member.domain.jwt.api;

import click.dailyfeed.code.domain.member.key.exception.JwtKeyException;
import click.dailyfeed.code.global.web.response.DailyfeedErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "click.dailyfeed.member.domain.key.api")
public class TokenControllerAdvice {

    @ExceptionHandler(JwtKeyException.class)
    public DailyfeedErrorResponse handleJwtKeyException(JwtKeyException e, HttpServletRequest request) {
        return DailyfeedErrorResponse.of(
                e.getJwtKeyExceptionCode().getMessage(),
                e.getJwtKeyExceptionCode().getReason(),
                e.getJwtKeyExceptionCode().getCode(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public DailyfeedErrorResponse handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        return DailyfeedErrorResponse.of(
                "서버 내부 오류가 발생했습니다.",
                "INTERNAL_SERVER_ERROR",
                500,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public DailyfeedErrorResponse handleException(Exception e, HttpServletRequest request) {
        return DailyfeedErrorResponse.of(
                "서버 내부 오류가 발생했습니다.",
                "INTERNAL_SERVER_ERROR",
                500,
                request.getRequestURI()
        );
    }
}
