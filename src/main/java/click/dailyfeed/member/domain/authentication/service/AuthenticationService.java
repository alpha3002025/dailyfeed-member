package click.dailyfeed.member.domain.authentication.service;

import click.dailyfeed.code.domain.authentication.code.AuthenticationExceptionCode;
import click.dailyfeed.code.domain.authentication.exception.AuthenticationException;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberAlreadyExistsException;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.domain.member.member.exception.MemberPasswordInvalidException;
import click.dailyfeed.code.domain.member.member.predicate.MemberExistsPredicate;
import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.authentication.mapper.AuthenticationMapper;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.mapper.JwtMapper;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.jwt.service.TokenService;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.entity.MemberProfile;
import click.dailyfeed.member.domain.member.mapper.MemberProfileMapper;
import click.dailyfeed.member.domain.member.repository.jpa.MemberProfileRepository;
import click.dailyfeed.member.domain.member.repository.jpa.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
    private final MemberProfileRepository memberProfileRepository;
    private final JwtKeyHelper jwtKeyHelper;
    private final TokenService tokenService;
    private final AuthenticationMapper authenticationMapper;
    private final MemberProfileMapper memberProfileMapper;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Transactional
    public DailyfeedServerResponse<MemberProfileDto.Summary> login(
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

        MemberProfile memberProfile = memberProfileRepository
                .findMemberProfileByMemberId(member.getId())
                .orElseThrow(MemberNotFoundException::new);

        return DailyfeedServerResponse.<MemberProfileDto.Summary>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(memberProfileMapper.fromEntityToSummary(memberProfile))
                .build();
    }

    public Boolean deactivate(Long id) {
        memberRepository.deleteById(id);
        return false;
    }

    public DailyfeedServerResponse<MemberDto.Member> signup(AuthenticationDto.SignupRequest signupRequest) {
        if (MemberExistsPredicate.EXISTS.equals(checkIfMemberAlreadyExists(signupRequest))) {
            throw new MemberAlreadyExistsException();
        }

        Member newMember = authenticationMapper.newMember(signupRequest, passwordEncoder, "MEMBER");
        Member saved = memberRepository.save(newMember);

        return DailyfeedServerResponse.<MemberDto.Member>builder()
                .status(HttpStatus.CREATED.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(authenticationMapper.fromMemberEntityToMemberDto(saved))
                .build();
    }

    public DailyfeedServerResponse<Boolean> logout(HttpServletRequest request, HttpServletResponse response) {
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

            return DailyfeedServerResponse.<Boolean>builder()
                    .status(HttpStatus.OK.value())
                    .result(ResponseSuccessCode.SUCCESS)
                    .data(Boolean.TRUE)
                    .build();
        } catch (Exception e){
            log.error("Logout error: {}", e.getMessage());
            throw new AuthenticationException(AuthenticationExceptionCode.LOGOUT_FAIL_BAD_REQUEST);
        }
    }

    public DailyfeedServerResponse<AuthenticationDto.TokenRefreshResponse> refreshToken(
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

            AuthenticationDto.TokenRefreshResponse refeshResponse = AuthenticationDto.TokenRefreshResponse.builder()
                    .accessToken(tokenPair.getAccessToken())
                    .expiresIn(tokenPair.getAccessTokenExpiresIn())
                    .build();

            return DailyfeedServerResponse.<AuthenticationDto.TokenRefreshResponse>builder()
                    .status(HttpStatus.OK.value())
                    .result(ResponseSuccessCode.SUCCESS)
                    .data(refeshResponse)
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
                .secure(isHttpsSupported())
                .sameSite(sameSite())
                .path("/")
                .maxAge(30 * 24 * 60 * 60)
                .build();

        String cookieString = cookie.toString();
        log.info("🍪 Setting refresh token cookie: {}", cookieString);
        log.info("   Profile: {}, Secure: {}, SameSite: {}", activeProfile, isHttpsSupported(), sameSite());
        response.addHeader("Set-Cookie", cookieString);
    }

    protected void removeRefreshTokenCookie(HttpServletResponse response) {
        // local-was: SameSite=None (다른 포트 허용)
        // 그 외: SameSite=Strict (같은 도메인만)
        String sameSite = "local-was".equals(activeProfile) ? "None" : "Strict";

        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(isHttpsSupported())
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String sameSite(){
        // local-was: SameSite=Lax (Chrome의 SameSite=None + Secure=false 거부 문제 회피)
        // 그 외: SameSite=Strict (같은 도메인만)
        if("local-was".equals(activeProfile)) return "Lax";
        else return "Strict";
    }

    public boolean isHttpsSupported(){
        if("local-was".equals(activeProfile)){
            return false;
        }
        return true;
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
        return memberRepository
                .findFirstByEmailFetchJoin(loginRequest.getEmail())
                .orElseThrow(() -> new MemberNotFoundException());
    }

    public void checkIfPasswordMatchesOrThrow(String requestPassword, String encryptedPassword) {
        if (!passwordEncoder.matches(requestPassword, encryptedPassword)) {
            throw new MemberPasswordInvalidException();
        }
    }

    @Transactional(readOnly = true)
    public MemberExistsPredicate checkIfMemberAlreadyExists(AuthenticationDto.SignupRequest signupRequest) {
        if (memberRepository.findFirstByEmailFetchJoin(signupRequest.getEmail()).isPresent()) {
            return MemberExistsPredicate.EXISTS;
        }
        return MemberExistsPredicate.NOT_EXISTS;
    }
}
