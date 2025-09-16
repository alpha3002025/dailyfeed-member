package click.dailyfeed.member.domain.jwt.service;

import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.entity.RefreshToken;
import click.dailyfeed.member.domain.jwt.entity.TokenBlacklist;
import click.dailyfeed.member.domain.jwt.mapper.JwtMapper;
import click.dailyfeed.member.domain.jwt.repository.RefreshTokenRepository;
import click.dailyfeed.member.domain.jwt.repository.TokenBlacklistRepository;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class TokenService {

    private final JwtKeyHelper jwtKeyHelper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final MemberRepository memberRepository;

    @Value("${jwt.refresh.expiration.days:30}")
    private Integer refreshTokenExpirationDays;

    @Value("${jwt.access.expiration.hours:1}")
    private Integer accessTokenExpirationHours;

    /**
     * 액세스 토큰과 리프레시 토큰 쌍 생성
     */
    public TokenPair generateTokenPair(JwtDto.UserDetails userDetails, String deviceInfo, String ipAddress) {
        // ID 생성
        String tokenId = generateTokenId();
        String jti = generateJti();
        String refreshTokenValue = generateRefreshTokenValue();

        // 날짜 생성
        LocalDateTime currentTime = getCurrentTime();
        LocalDateTime refreshTokenExpiration = generateRefreshTokenExpiration();

        // 액세스 토큰 생성 (JTI 포함, 만료 시간은 JwtKeyHelper에서 생성)
        String accessToken = jwtKeyHelper.generateTokenWithJti(userDetails, jti);

        // 리프레시 토큰 엔티티 생성
        RefreshToken refreshToken = RefreshToken.create(
                tokenId,
                userDetails.getId(),
                refreshTokenValue,
                jti,
                refreshTokenExpiration,
                deviceInfo,
                ipAddress
        );

        refreshTokenRepository.save(refreshToken);

        return new TokenPair(
                accessToken,
                refreshTokenValue,
                accessTokenExpirationHours * 3600L,
                refreshTokenExpirationDays * 86400L
        );
    }

    /**
     * 리프레시 토큰으로 새로운 토큰 쌍 발급
     */
    public TokenPair refreshTokens(String refreshTokenValue, String deviceInfo, String ipAddress) {
        LocalDateTime currentTime = getCurrentTime();

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenValueAndIsRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!refreshToken.isValidAt(currentTime)) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }

        // 기존 리프레시 토큰 무효화
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        // 사용자 정보 조회
        Member member = memberRepository.findById(refreshToken.getMemberId())
                .orElseThrow(() -> new InvalidTokenException("Member not found"));

        // UserDetails 생성 (만료 시간은 JwtKeyHelper에서 처리)
        JwtDto.UserDetails userDetails = JwtMapper.ofUserDetails(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                jwtKeyHelper.generateAccessTokenExpiration()
        );

        // 새로운 토큰 쌍 생성
        return generateTokenPair(userDetails, deviceInfo, ipAddress);
    }

    /**
     * 로그아웃 처리
     */
    public void logout(String accessToken, Long memberId) {
        try {
            // 액세스 토큰에서 정보 추출
            String jti = jwtKeyHelper.extractJti(accessToken);
            Date expirationDate = jwtKeyHelper.extractExpiration(accessToken);
            LocalDateTime expiresAt = convertToLocalDateTime(expirationDate);

            // 블랙리스트에 추가
            TokenBlacklist blacklistedToken = TokenBlacklist.create(
                    jti,
                    memberId,
                    expiresAt,
                    "USER_LOGOUT"
            );
            tokenBlacklistRepository.save(blacklistedToken);

            // 연관된 리프레시 토큰 무효화
            refreshTokenRepository.findByAccessTokenIdAndIsRevokedFalse(jti)
                    .ifPresent(refreshToken -> {
                        refreshToken.revoke();
                        refreshTokenRepository.save(refreshToken);
                    });

            log.info("User {} logged out successfully. Token JTI: {}", memberId, jti);

        } catch (Exception e) {
            log.error("Error during logout for user {}: {}", memberId, e.getMessage());
            // 모든 리프레시 토큰 무효화 (fallback)
            refreshTokenRepository.revokeAllByMemberId(memberId);
        }
    }

    /**
     * 모든 디바이스에서 로그아웃
     */
    public void logoutAllDevices(Long memberId) {
        refreshTokenRepository.revokeAllByMemberId(memberId);
        log.info("All devices logged out for user {}", memberId);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isTokenBlacklisted(String jti) {
        return tokenBlacklistRepository.existsByJti(jti);
    }

    /**
     * 만료된 토큰 정리 (스케줄러)
     */
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    public void cleanupExpiredTokens() {
        LocalDateTime now = getCurrentTime();

        refreshTokenRepository.deleteExpiredTokens(now);
        tokenBlacklistRepository.deleteExpiredTokens(now);

        log.info("Expired tokens cleanup completed at {}", now);
    }

    // 테스트 가능한 메서드들
    protected LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    protected LocalDateTime generateRefreshTokenExpiration() {
        return getCurrentTime().plusDays(refreshTokenExpirationDays);
    }

    protected String generateTokenId() {
        return UUID.randomUUID().toString();
    }

    protected String generateJti() {
        return UUID.randomUUID().toString();
    }

    protected String generateRefreshTokenValue() {
        return UUID.randomUUID().toString();
    }

    protected LocalDateTime convertToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    // Inner class for token pair
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
        private final Long accessTokenExpiresIn;
        private final Long refreshTokenExpiresIn;

        public TokenPair(String accessToken, String refreshToken,
                         Long accessTokenExpiresIn, Long refreshTokenExpiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.accessTokenExpiresIn = accessTokenExpiresIn;
            this.refreshTokenExpiresIn = refreshTokenExpiresIn;
        }

        // Getters
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public Long getAccessTokenExpiresIn() { return accessTokenExpiresIn; }
        public Long getRefreshTokenExpiresIn() { return refreshTokenExpiresIn; }
    }
}
