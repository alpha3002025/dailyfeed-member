package click.dailyfeed.member.domain.jwt.service;

import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.domain.member.member.predicate.BlackListedPredicate;
import click.dailyfeed.code.global.cache.RedisKeyPrefix;
import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.entity.RefreshToken;
import click.dailyfeed.member.domain.jwt.entity.TokenBlacklist;
import click.dailyfeed.member.domain.jwt.mapper.JwtMapper;
import click.dailyfeed.member.domain.jwt.repository.jpa.RefreshTokenRepository;
import click.dailyfeed.member.domain.jwt.repository.jpa.TokenBlacklistRepository;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.jpa.MemberRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

    @Qualifier("stringRedisTemplate")
    private final RedisTemplate<String, String> stringRedisTemplate;

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
        // Redis에서 무효화 여부 먼저 확인
        String revokedRefreshKey = revokedRefreshTokenKey(refreshTokenValue);
        try {
            Boolean isRevoked = stringRedisTemplate.hasKey(revokedRefreshKey);
            if (Boolean.TRUE.equals(isRevoked)) {
                log.debug("Refresh token found in Redis revocation list");
                throw new InvalidTokenException("Refresh token has been revoked");
            }
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis check failed, continuing with DB check: {}", e.getMessage());
        }

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

        // Redis에도 무효화 마킹
        try {
            stringRedisTemplate.opsForValue().set(
                    revokedRefreshKey,
                    "true",
                    Duration.ofDays(30)
            );
        } catch (Exception e) {
            log.warn("Failed to mark refresh token as revoked in Redis: {}", e.getMessage());
        }

        // 사용자 정보 조회
        List<Member> result = memberRepository.findByIdFetchJoin(refreshToken.getMemberId());
        if (result.isEmpty()) {
            throw new MemberNotFoundException();
        }

        Member member = result.get(0);

        // UserDetails 생성 (만료 시간은 JwtKeyHelper에서 처리)
        JwtDto.UserDetails userDetails = JwtMapper.ofUserDetails(
                member.getId(),
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
            String keyId = JwtProcessor.extractKeyIdOrThrow(accessToken);

            // 액세스 토큰에서 정보 추출
            Claims claims = jwtKeyHelper.readClaim(keyId, accessToken);
            String jti = jwtKeyHelper.extractJti(claims);
            Date expirationDate = jwtKeyHelper.extractExpiration(claims);
            LocalDateTime expiresAt = convertToLocalDateTime(expirationDate);

            long ttlSeconds = calculateTTL(expirationDate);
            stringRedisTemplate.opsForValue().set(blacklistedJtiRedisKey(jti), String.valueOf(memberId), Duration.ofSeconds(ttlSeconds));
            log.info("Token added to Redis blacklist. JTI: {}, TTL: {}s", jti, ttlSeconds);

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
        // DB 에서 모든 Refresh Token 조회 후 Redis 에 무효화
        refreshTokenRepository.findAllByMemberIdAndIsRevokedFalse(memberId)
                .forEach(revokedRefreshToken -> {
                    try{
                        stringRedisTemplate.opsForValue().set(
                                revokedRefreshTokenKey(revokedRefreshToken.getTokenValue()),
                                "true",
                                Duration.ofDays(30)
                        );
                    } catch (Exception e){
                        log.warn("Failed to revoke token in Redis: {}", e.getMessage());
                    }
                });

        // DB 무효화 처리
        refreshTokenRepository.revokeAllByMemberId(memberId);
        log.info("All devices logged out for user {}", memberId);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public BlackListedPredicate isTokenBlacklisted(String jti) {
        try{
            // 1. Redis 에서 먼저 체크
            Boolean isKeyExists = stringRedisTemplate.hasKey(blacklistedJtiRedisKey(jti));
            if(Boolean.TRUE.equals(isKeyExists)){
                return BlackListedPredicate.BLACKLISTED;
            }

            // 2. Redis 에 없을 경우 DB 확인
            Optional<TokenBlacklist> blacklist = tokenBlacklistRepository.findByJti(jti);
            if (blacklist.isEmpty()) {
                return BlackListedPredicate.NOT_BLACKLISTED;
            }

            // 3. DB에 존재함 → 블랙리스트
            TokenBlacklist token = blacklist.get();
            Long ttl = calculateLocalDateTimeTTL(token.getExpiresAt());

            // 4. TTL이 남아있으면 Redis에 캐싱 (다음 조회 최적화)
            if (ttl > 0){
                stringRedisTemplate.opsForValue()
                        .set(
                                blacklistedJtiRedisKey(token.getJti()),
                                String.valueOf(token.getMemberId()),
                                Duration.ofSeconds(ttl)
                        );
                log.debug("Token cached to Redis with TTL: {} seconds", ttl);
            }
            else{
                log.debug("Token already expired, skipping Redis cache");
            }

            // 5. TTL과 관계없이 DB에 있으면 블랙리스트
            // (실제로는 Filter의 만료 체크에서 이미 차단되므로 여기까지 오지 않음)
            return BlackListedPredicate.BLACKLISTED;
        } catch (Exception e){
            return tokenBlacklistRepository.existsByJti(jti) ? BlackListedPredicate.BLACKLISTED : BlackListedPredicate.NOT_BLACKLISTED;
        }
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

    /// 테스트 가능한 메서드들
    /**
     * Redis Key 생성 (RefreshToken) : member:authentication:revoked_refresh:
     */
    public String revokedRefreshTokenKey(String refreshTokenValue){
        return String.format("%s%s", RedisKeyPrefix.MEMBER_AUTHENTICATION_REVOKED_REFRESH, refreshTokenValue);
    }

    /**
     * Redis Key 생성 (Blacklist) : member:authentication:blacklist:
     */
    public String blacklistedJtiRedisKey(String jti){
        return String.format("%s%s", RedisKeyPrefix.MEMBER_AUTHENTICATION_BLACKLIST, jti);
    }

    /**
     * Redis Key 를 얼마나 오랫동안 유지시킬지를 계산
     * ttl = expiration(만료시각) - 현재시각
     */
    protected Long calculateLocalDateTimeTTL(LocalDateTime expiredAt){
        Long expiredAtMs = expiredAt.atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();

        return Math.max(expiredAtMs - System.currentTimeMillis(), 0); // 음수 방지
    }

    /**
     * Redis Key 를 얼마나 오랫동안 유지시킬지를 계산
     * ttl = expiration(만료시각) - 현재시각
     */
    protected Long calculateTTL(Date expirationDate) {
        long ttl = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(ttl, 0); // 음수 방지
    }

    /**
     * 현재시각 반환
     */
    protected LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    /**
     * 현재시간 기준으로 Refresh Token 의 만료 기한 지정
     */
    protected LocalDateTime generateRefreshTokenExpiration() {
        return getCurrentTime().plusDays(refreshTokenExpirationDays);
    }

    /**
     * Token Id 생성
     */
    protected String generateTokenId() {
        return UUID.randomUUID().toString();
    }

    /**
     * JWT ID 생성
     */
    protected String generateJti() {
        return UUID.randomUUID().toString();
    }

    /**
     * Refresh Token 값 생성
     */
    protected String generateRefreshTokenValue() {
        return UUID.randomUUID().toString();
    }

    /**
     * Date 를 LocalDateTime 으로 변환
     */
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
