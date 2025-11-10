package click.dailyfeed.member.config.security.filter;

import click.dailyfeed.code.domain.member.member.code.MemberHeaderCode;
import click.dailyfeed.code.domain.member.member.predicate.BlackListedPredicate;
import click.dailyfeed.code.global.jwt.predicate.JwtExpiredPredicate;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.jwt.service.TokenService;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import io.jsonwebtoken.Claims;
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
        String path = request.getRequestURI();

        // 인증이 필요하지 않은 엔드포인트는 JWT 검증을 건너뜀
        if (shouldSkipAuthentication(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.debug("JWT Filter - Path: {}, Has Auth Header: {}", path, token != null);

        // JWT 토큰 추출 (Bearer 제거)
        token = JwtProcessor.getJwtFromHeaderOrThrow(token);

        // key id 추출
        String keyId = JwtProcessor.extractKeyIdOrThrow(token);
        Claims claims = jwtKeyHelper.readClaim(keyId, token);

        // JTI 추출
        String jti = jwtKeyHelper.extractJti(claims);

        // 블랙리스트 확인
        if (BlackListedPredicate.BLACKLISTED.equals(tokenService.isTokenBlacklisted(jti))) {
            log.debug("Token is blacklisted: JTI={}", jti);
            addReLoginRequiredAtResponseHeader(response);
            return;
        }

        // 토큰 검증 및 사용자 정보 추출
        JwtDto.UserDetails userDetails = jwtKeyHelper.readUserDetailsFromToken(keyId, token);

        // Access Token 만료 확인
        if (JwtExpiredPredicate.EXPIRED.equals(JwtProcessor.checkIfExpired(userDetails.getExpiration()))) {
            // Access Token 만료됨 -> Refresh Token 확인
            boolean hasCookie = hasRefreshTokenCookie(request);

            if (!hasCookie) {
                // 쿠키 없음 (서비스 간 통신) -> 토큰 갱신 필요
                log.debug("Access Token expired, no cookie (service-to-service) - MemberId: {}", userDetails.getId());
                addRefreshNeededAtResponseHeader(response);
                return;
            }

            // 쿠키 있음 (브라우저 요청) -> Refresh Token 검증
            JwtExpiredPredicate refreshTokenStatus = jwtKeyHelper.checkRefreshTokenExpiration(request);

            if (JwtExpiredPredicate.EXPIRED.equals(refreshTokenStatus)) {
                // Refresh Token도 만료됨 -> 재로그인 필요
                log.warn("Both Access and Refresh Token expired - MemberId: {}", userDetails.getId());
                addReLoginRequiredAtResponseHeader(response);
                return;
            }

            // Refresh Token은 유효함 -> Access Token 갱신 필요
            log.debug("Access Token expired, Refresh Token valid - MemberId: {}", userDetails.getId());
            addRefreshNeededAtResponseHeader(response);
            return;
        }

        // Access Token이 유효한 경우: 정상 처리
        // Spring Security 인증 설정
        cachingAuthenticationAtSecurityContext(userDetails.getId());

        // 만료된 JWT 생성 Key 로 만든 JWT 일 경우 (401 응답 x -> X-Token-Refresh-Needed 만 응답헤더에 심어서 응답)
        jwtKeyHelper.checkAndRefreshHeader(token, response);
        filterChain.doFilter(request, response);
    }

    public void addReLoginRequiredAtResponseHeader(HttpServletResponse response) {
        String headerKey = MemberHeaderCode.X_RELOGIN_REQUIRED.getHeaderKey();
        response.setHeader(headerKey, "true");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    public void addRefreshNeededAtResponseHeader(HttpServletResponse response) {
        String headerKey = MemberHeaderCode.X_TOKEN_REFRESH_NEEDED.getHeaderKey();
        response.setHeader(headerKey, "true");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    public void cachingAuthenticationAtSecurityContext(Long memberId){
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(memberId),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private boolean hasRefreshTokenCookie(HttpServletRequest request) {
        return jwtKeyHelper.extractRefreshTokenFromCookie(request) != null;
    }

    /**
     * 요청자가 브라우저인지 서비스인지 구분
     * - 쿠키가 있으면 브라우저 요청으로 간주
     * - 쿠키가 없으면 서비스 간 통신으로 간주
     * 추가 옵션: User-Agent 또는 커스텀 헤더(X-Service-Name 등)로 구분 가능
     */
    private boolean isServiceToServiceRequest(HttpServletRequest request) {
        // 쿠키 기반 판단: 쿠키가 없으면 서비스 간 통신
        boolean hasCookie = hasRefreshTokenCookie(request);
        if (!hasCookie) {
            return true;
        }

        // 추가 옵션: 커스텀 헤더로 명시적 구분 (필요시 활성화)
        // String serviceHeader = request.getHeader("X-Service-Name");
        // if (serviceHeader != null) {
        //     return true;
        // }

        // 추가 옵션: User-Agent로 구분 (필요시 활성화)
        // String userAgent = request.getHeader("User-Agent");
        // if (userAgent != null && userAgent.contains("Java")) {
        //     return true;
        // }

        return false;
    }

    private boolean shouldSkipAuthentication(String path) {
        return path.equals("/") ||
               path.startsWith("/welcome") ||
               path.startsWith("/img/") ||
               path.startsWith("/css/") ||
               path.equals("/api/authentication/login") ||
               path.equals("/api/authentication/signup") ||
               path.equals("/api/authentication/refresh") ||
               path.equals("/api/token/refresh") ||  // Token refresh endpoint
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/swagger-example/") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/app-health/") ||  // Istio rewritten health checks (port 15020)
               path.startsWith("/healthz") ||       // Kubernetes/Istio health checks
               path.startsWith("/healthcheck/") ||  // Application health checks
               path.equals("/healthcheck/ready") ||
               path.equals("/healthcheck/live") ||
               path.equals("/healthcheck/startup") ||
               path.startsWith("/actuator/health"); // Spring Boot Actuator health
    }
}
