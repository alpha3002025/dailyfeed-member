package click.dailyfeed.member.domain.authentication.service;

import click.dailyfeed.code.domain.member.member.exception.MemberAlreadyExistsException;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.domain.member.member.exception.MemberPasswordInvalidException;
import click.dailyfeed.code.domain.member.member.predicate.MemberExistsPredicate;
import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.authentication.mapper.AuthenticationMapper;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.mapper.JwtMapper;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.jwt.service.TokenService;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final AuthenticationMapper authenticationMapper;
    private final JwtKeyHelper jwtKeyHelper;
    private final TokenService tokenService;

    @Transactional
    public AuthenticationDto.LoginResponse login(
            AuthenticationDto.LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        Member member = getMemberOrThrow(loginRequest);
        String encryptedPassword = member.getPassword();

        checkIfPasswordMatchesOrThrow(loginRequest.getPassword(), encryptedPassword);

        // UserDetails 생성 (만료 시간은 JwtKeyHelper에서 생성)
        Date expirationDate = jwtKeyHelper.generateAccessTokenExpiration();
        JwtDto.UserDetails userDetails = JwtMapper.ofUserDetails(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                expirationDate
        );

        // 디바이스 정보, IP 추출
        String deviceInfo = extractDeviceInfo(request);
        String ipAddress = extractIpAddress(request);

        // 토큰 쌍 생성
        TokenService.TokenPair tokenPair = tokenService.generateTokenPair(
                userDetails,
                deviceInfo,
                ipAddress
        );

        // Response에 토큰 설정
        JwtProcessor.addJwtAtResponseHeader(tokenPair.getAccessToken(), response);

        // 리프레시 토큰은 HttpOnly 쿠키로 설정
        setRefreshTokenCookie(response, tokenPair.getRefreshToken());
        return authenticationMapper.ofLoginSuccess();
    }

    public AuthenticationDto.SignupResponse signup(AuthenticationDto.SignupRequest signupRequest) {
        if (MemberExistsPredicate.EXISTS.equals(checkIfMemberAlreadyExists(signupRequest))) {
            throw new MemberAlreadyExistsException();
        }

        Member newMember = authenticationMapper.newMember(signupRequest, passwordEncoder, "MEMBER");
        Member saved = memberRepository.save(newMember);
        return authenticationMapper.ofSignupSuccess();
    }

    public AuthenticationDto.LogoutResponse logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String accessToken = authHeader.substring(7);
                Long memberId = jwtKeyHelper.extractMemberId(accessToken);

                // 로그아웃 처리
                tokenService.logout(accessToken, memberId);
            }

            // Spring Security 로그아웃
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                SecurityContextHolder.clearContext();
            }

            // 쿠키 제거
            removeRefreshTokenCookie(response);
            response.setHeader("Authorization", "");

            return authenticationMapper.ofLogoutSuccessResponse("LOGOUT_SUCCESS");
        } catch (Exception e){
            log.error("Logout error: {}", e.getMessage());
            return authenticationMapper.ofLogoutSuccessResponse("LOGOUT_FAILED");
        }
    }

    public AuthenticationDto.TokenRefreshResponse refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            String refreshToken = extractRefreshTokenFromCookie(request);
            String deviceInfo = extractDeviceInfo(request);
            String ipAddress = extractIpAddress(request);

            TokenService.TokenPair tokenPair = tokenService.refreshTokens(
                    refreshToken,
                    deviceInfo,
                    ipAddress
            );

            JwtProcessor.addJwtAtResponseHeader(tokenPair.getAccessToken(), response);
            setRefreshTokenCookie(response, tokenPair.getRefreshToken());

            return AuthenticationDto.TokenRefreshResponse.builder()
                    .success(true)
                    .accessToken(tokenPair.getAccessToken())
                    .expiresIn(tokenPair.getAccessTokenExpiresIn())
                    .build();

        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            throw new InvalidTokenException("Token refresh failed");
        }
    }

    // Helper 메서드들 (테스트 가능하도록 protected)
    protected void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(30 * 24 * 60 * 60)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    protected void removeRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    protected String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        }
        throw new InvalidTokenException("No cookies found");
    }

    protected String extractDeviceInfo(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    protected String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Transactional(readOnly = true)
    public Member getMemberOrThrow(AuthenticationDto.LoginRequest loginRequest) {
        Member member = memberRepository.findByEmail(loginRequest.getEmail());
        if (member == null) {
            throw new MemberNotFoundException();
        }
        return member;
    }

    public void checkIfPasswordMatchesOrThrow(String requestPassword, String encryptedPassword) {
        if (!passwordEncoder.matches(requestPassword, encryptedPassword)) {
            throw new MemberPasswordInvalidException();
        }
    }

    @Transactional(readOnly = true)
    public MemberExistsPredicate checkIfMemberAlreadyExists(AuthenticationDto.SignupRequest signupRequest) {
        if (memberRepository.findByEmail(signupRequest.getEmail()) == null) {
            return MemberExistsPredicate.NOT_EXISTS;
        }
        return MemberExistsPredicate.EXISTS;
    }
}
