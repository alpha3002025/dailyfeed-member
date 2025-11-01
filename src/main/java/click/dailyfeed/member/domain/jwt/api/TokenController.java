package click.dailyfeed.member.domain.jwt.api;

import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.domain.jwt.service.TokenService;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Slf4j
@RequestMapping("/api/token")
@RestController
@RequiredArgsConstructor
public class TokenController {
    private final TokenService tokenService;

    @PostMapping("/refresh")
    public DailyfeedServerResponse<Boolean> refreshKey(
            HttpServletRequest request,
            HttpServletResponse response
    ){
        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = extractRefreshTokenFromCookie(request);

        // 2. 디바이스 정보 및 IP 추출
        String deviceInfo = extractDeviceInfo(request);
        String ipAddress = extractIpAddress(request);

        // 3. TokenService를 통해 새로운 토큰 쌍 생성 (Refresh Token Rotation)
        TokenService.TokenPair tokenPair = tokenService.refreshTokens(
                refreshToken,
                deviceInfo,
                ipAddress
        );

        // 4. Response Header에 Access Token 추가
        JwtProcessor.addJwtAtResponseHeader(tokenPair.getAccessToken(), response);

        // 5. Response Cookie에 새로운 Refresh Token 추가
        setRefreshTokenCookie(response, tokenPair.getRefreshToken());

        log.info("Token refreshed successfully via /api/token/refresh");

        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(Boolean.TRUE)
                .build();
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        }
        throw new InvalidTokenException("No cookies found");
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(30 * 24 * 60 * 60)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String extractDeviceInfo(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
