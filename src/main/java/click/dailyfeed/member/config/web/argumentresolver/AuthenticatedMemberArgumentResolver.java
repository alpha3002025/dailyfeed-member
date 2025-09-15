package click.dailyfeed.member.config.web.argumentresolver;

import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.jwt.exception.BearerTokenMissingException;
import click.dailyfeed.member.config.web.annotation.AuthenticatedMember;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.member.redis.MemberRedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class AuthenticatedMemberArgumentResolver implements HandlerMethodArgumentResolver {
    private final JwtKeyHelper jwtKeyHelper;
    private final MemberRedisService memberRedisService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedMember.class) &&
                parameter.getParameterType().equals(MemberDto.Member.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);

        /// jwt 추출
        String authHeader = request.getHeader("Authorization");
        String jwt = extractToken(authHeader);

        /// Member 존재하는지 체크
        JwtDto.UserDetails userDetails = jwtKeyHelper.validateAndParseToken(jwt);

        /// Key Refresh 필요한지 체크
        jwtKeyHelper.checkAndRefreshHeader(jwt, response);

        return memberRedisService.getMemberOrThrow(userDetails.getId());
    }

    public String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            throw new BearerTokenMissingException();
        }

        return authHeader.replace("Bearer ", "");
    }
}
