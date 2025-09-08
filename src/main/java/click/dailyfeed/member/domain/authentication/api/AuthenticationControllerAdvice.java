package click.dailyfeed.member.domain.authentication.api;

import click.dailyfeed.code.domain.member.member.exception.MemberException;
import click.dailyfeed.code.global.web.response.DailyfeedErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
                e.getMemberExceptionCode().getMessage(),
                e.getMemberExceptionCode().getReason(),
                e.getMemberExceptionCode().getCode(),
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
                "서버 내부 오류가 발생했습니다",
                "INTERNAL_SERVER_ERROR",
                500,
                request.getRequestURI()
        );
    }

}
