package click.dailyfeed.member.domain.jwt.service;

import click.dailyfeed.code.domain.member.member.code.MemberHeaderCode;
import click.dailyfeed.code.global.jwt.exception.BearerTokenMissingException;
import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.code.global.jwt.exception.JwtExpiredException;
import click.dailyfeed.code.global.jwt.predicate.JwtExpiredPredicate;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.util.Date;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Component
public class JwtKeyHelper {
    private final JwtKeyRotationService jwtKeyRotationService;

    @Value("${jwt.access.expiration.hours:1}")
    private Integer accessTokenExpirationHours;

    /**
     * 액세스 토큰 만료 시간 생성
     */
    public Date generateAccessTokenExpiration() {
        return new Date(System.currentTimeMillis() + (accessTokenExpirationHours * 3600000L));
    }

    /**
     * 커스텀 만료 시간 생성
     */
    public Date generateExpirationDate(long durationMillis) {
        return new Date(System.currentTimeMillis() + durationMillis);
    }

    /**
     * JTI를 포함한 토큰 생성 (만료 시간 자동 생성)
     */
    public String generateTokenWithJti(JwtDto.UserDetails userDetails, String jti) {
        Key primaryKey = jwtKeyRotationService.getPrimaryKey();
        String primaryKeyId = jwtKeyRotationService.getPrimaryKeyId();
        Date expirationDate = generateAccessTokenExpiration();

        return Jwts.builder()
                .setHeaderParam("kid", primaryKeyId)
                .setId(jti)  // JTI 설정
                .setSubject(userDetails.getEmail())
                .setExpiration(expirationDate)
                .claim("id", userDetails.getId())
                .claim("email", userDetails.getEmail())
                .claim("password", userDetails.getPassword())
                .signWith(primaryKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 JTI 추출
     */
    public String extractJti(String token) {
        String keyId = JwtProcessor.extractKeyIdOrThrow(token);
        Key key = jwtKeyRotationService.getKeyByKeyId(keyId);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getId();
    }

    /**
     * 토큰에서 만료 시간 추출
     */
    public Date extractExpiration(String token) {
        String keyId = JwtProcessor.extractKeyIdOrThrow(token);
        Key key = jwtKeyRotationService.getKeyByKeyId(keyId);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration();
    }

    /**
     * 토큰에서 Member ID 추출
     */
    public Long extractMemberId(String token) {
        JwtDto.UserDetails userDetails = validateAndParseToken(token);
        return userDetails.getId();
    }

    /**
     * 새로운 JWT 토큰 생성 (항상 Primary Key 사용)
     */
    public String generateToken(JwtDto.UserDetails userDetails) {
        Key primaryKey = jwtKeyRotationService.getPrimaryKey();
        String primaryKeyId = jwtKeyRotationService.getPrimaryKeyId();

        return JwtProcessor.generateToken(primaryKey, primaryKeyId, userDetails);
    }

    /**
     * JWT 토큰 검증 및 사용자 정보 추출
     */
    public JwtDto.UserDetails validateAndParseToken(String token) {
        // 1. 토큰에서 Key ID 추출
        String keyId = JwtProcessor.extractKeyIdOrThrow(token);

        // 2. Key ID로 해당 Key 조회
        Key key = jwtKeyRotationService.getKeyByKeyId(keyId);

        // 3. 토큰 검증 및 파싱
        return JwtProcessor.degenerateToken(key, token);
    }

    /**
     * Authorization 헤더에서 멤버 ID 추출
     */
    public Long getMemberIdFromAuthHeader(String authorizationHeader) {
        if (!JwtProcessor.checkContainsBearer(authorizationHeader)) {
            throw new BearerTokenMissingException();
        }

        String token = JwtProcessor.getJwtFromHeaderOrThrow(authorizationHeader);

        try {
            JwtDto.UserDetails userDetails = validateAndParseToken(token);

            // 토큰 만료 여부 확인
            if (JwtExpiredPredicate.EXPIRED.equals(JwtProcessor.checkIfExpired(userDetails.getExpiration()))) {
                throw new JwtExpiredException();
            }

            return userDetails.getId();
        } catch (Exception e) {
            throw new InvalidTokenException();
        }
    }

    /**
     * 토큰 갱신 (새로운 Primary Key로 토큰 재발급)
     */
    public String refreshTokenOrThrow(String authorizationHeader) {
        // JWT 토큰 추출
        String oldToken = JwtProcessor.getJwtFromHeaderOrThrow(authorizationHeader);

        // 기존 토큰에서 사용자 정보 추출
        JwtDto.UserDetails userDetails = validateAndParseToken(oldToken);

        if (JwtExpiredPredicate.EXPIRED.equals(JwtProcessor.checkIfExpired(userDetails.getExpiration()))) {
            throw new JwtExpiredException();
        }

        // JTI 생성하여 올바른 expiration 설정
        String jti = java.util.UUID.randomUUID().toString();
        return generateTokenWithJti(userDetails, jti);
    }

    /**
     * 갱신 필요 여부 체크 및 헤더 추가
     */
    public void checkAndRefreshHeader(String token, HttpServletResponse response) {
        String currentKeyId = JwtProcessor.extractKeyIdOrThrow(token);
        String primaryKeyId = jwtKeyRotationService.getPrimaryKeyId();

        if (!currentKeyId.equals(primaryKeyId)) {
            String headerKey = MemberHeaderCode.X_TOKEN_REFRESH_NEEDED.getHeaderKey();
            response.addHeader(headerKey, "true");
            log.info("Token refresh needed - Current: {}, Primary: {}", currentKeyId, primaryKeyId);
        }
    }
}
