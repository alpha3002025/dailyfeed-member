package click.dailyfeed.member.config.security.filter;

import click.dailyfeed.code.global.jwt.predicate.JwtExpiredPredicate;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.jwt.service.TokenService;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtKeyHelper jwtKeyHelper;
    private final TokenService tokenService;

    public JwtAuthenticationFilter(
            JwtKeyHelper jwtKeyHelper,
            TokenService tokenService
    ) {
        this.jwtKeyHelper = jwtKeyHelper;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            String token = JwtProcessor.getJwtFromHeaderOrThrow(header);

            // JTI 추출 및 블랙리스트 확인
            String jti = jwtKeyHelper.extractJti(token);
            if (tokenService.isTokenBlacklisted(jti)) {
                log.debug("Token is blacklisted: JTI={}", jti);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token has been revoked");
                return;
            }

            // 토큰 검증 및 사용자 정보 추출
            JwtDto.UserDetails userDetails = jwtKeyHelper.validateAndParseToken(token);

            // 만료 확인
            if (JwtExpiredPredicate.EXPIRED.equals(JwtProcessor.checkIfExpired(userDetails.getExpiration()))) {
                log.debug("Token expired for user: {}", userDetails.getEmail());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token expired");
                return;
            }

            // Spring Security 인증 설정
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails.getEmail(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 토큰 갱신 필요 여부 체크
            jwtKeyHelper.checkAndRefreshHeader(header, response);
        } catch (Exception e) {
            // 예외가 발생해도 필터 체인을 계속 진행 (인증 실패로 처리)
            log.error("JWT authentication failed: {}", e.getMessage());
            // 인증 실패 시 SecurityContext를 비움
            SecurityContextHolder.clearContext();

            // 보호된 엔드포인트 요청의 경우 401 반환
            String path = request.getRequestURI();
            if (path.startsWith("/api/authentication/logout") ||
                path.startsWith("/api/members/") ||
                path.startsWith("/api/token/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or missing token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
