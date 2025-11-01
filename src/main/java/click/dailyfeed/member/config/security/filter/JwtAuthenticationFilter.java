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

        if(token != null && !token.isBlank() && token.contains("Bearer ")) { // 로그인 되었을때(token != null)
            token = JwtProcessor.getJwtFromHeaderOrThrow(token);
        }

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

        // Refresh Token 만료 확인 (항상 체크)
        JwtExpiredPredicate refreshTokenStatus = jwtKeyHelper.checkRefreshTokenExpiration(request);

        // Refresh Token 만료 시 즉시 재로그인 요구
        if (JwtExpiredPredicate.EXPIRED.equals(refreshTokenStatus)) {
            log.warn("Refresh Token expired or revoked - MemberId: {}", userDetails.getId());
            addReLoginRequiredAtResponseHeader(response);
            return;
        }

        // Access Token 만료 확인
        if (JwtExpiredPredicate.EXPIRED.equals(JwtProcessor.checkIfExpired(userDetails.getExpiration()))) {
            // Access Token만 만료됨 -> 갱신 필요
            addRefreshNeededAtResponseHeader(response);
            return;
        }

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

//    private JwtExpiredPredicate checkRefreshTokenExpiration(HttpServletRequest request) {
//        try {
//            // 1. 쿠키에서 Refresh Token 추출
//            String refreshTokenValue = extractRefreshTokenFromCookie(request);
//            if (refreshTokenValue == null) {
//                log.debug("No refresh token found in cookie");
//                return JwtExpiredPredicate.EXPIRED;
//            }
//
//            // 2. DB에서 Refresh Token 조회
//            Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository
//                    .findByTokenValueAndIsRevokedFalse(refreshTokenValue);
//
//            if (refreshTokenOpt.isEmpty()) {
//                log.debug("Refresh token not found in DB or already revoked");
//                return JwtExpiredPredicate.EXPIRED;
//            }
//
//            RefreshToken refreshToken = refreshTokenOpt.get();
//
//            // 3. 만료 여부 확인
//            LocalDateTime now = LocalDateTime.now();
//            boolean isExpired = refreshToken.isExpiredAt(now) || !refreshToken.isValidAt(now);
//
//            if (isExpired) {
//                log.debug("Refresh token expired at: {}", refreshToken.getExpiresAt());
//                return JwtExpiredPredicate.EXPIRED;
//            }
//
//            return JwtExpiredPredicate.NOT_EXPIRED;
//
//        } catch (Exception e) {
//            log.error("Error checking refresh token expiration: {}", e.getMessage());
//            return JwtExpiredPredicate.EXPIRED; // 에러 발생 시 만료로 간주
//        }
//    }
//
//    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
//        if (request.getCookies() != null) {
//            return Arrays.stream(request.getCookies())
//                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
//                    .map(Cookie::getValue)
//                    .findFirst()
//                    .orElse(null);
//        }
//        return null;
//    }

    private boolean shouldSkipAuthentication(String path) {
        return path.equals("/") ||
               path.startsWith("/welcome") ||
               path.startsWith("/img/") ||
               path.startsWith("/css/") ||
               path.equals("/api/authentication/login") ||
               path.equals("/api/authentication/signup") ||
               path.equals("/api/authentication/refresh") ||
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
