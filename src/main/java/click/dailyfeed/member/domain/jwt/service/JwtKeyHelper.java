package click.dailyfeed.member.domain.jwt.service;

import click.dailyfeed.code.global.jwt.exception.BearerTokenMissingException;
import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.code.global.jwt.exception.JwtExpiredException;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Component
public class JwtKeyHelper {
    private final JwtKeyRotationService jwtKeyRotationService;

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

        String token = authorizationHeader.substring(7); // "Bearer " 제거

        try {
            JwtDto.UserDetails userDetails = validateAndParseToken(token);

            // 토큰 만료 여부 확인
            if (!JwtProcessor.checkIfExpired(userDetails.getExpiration())) {
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
        if (!JwtProcessor.checkContainsBearer(authorizationHeader)) {
            throw new InvalidTokenException("Invalid Toke Format");
        }

        // JWT 토큰 추출
        String oldToken = authorizationHeader.substring(7);

        // 기존 토큰에서 사용자 정보 추출
        JwtDto.UserDetails userDetails = validateAndParseToken(oldToken);

        if (!JwtProcessor.checkIfExpired(userDetails.getExpiration())) {
            throw new JwtExpiredException();
        }

        return generateToken(userDetails);
    }

    /**
     * 갱신 필요 여부 체크 및 헤더 추가
     */
    public void checkAndRefreshHeader(String token, HttpServletResponse response) {
        try{
            if(JwtProcessor.checkContainsBearer(token)){
                String currentToken = token.substring(7);
                String currentKeyId = JwtProcessor.extractKeyIdOrThrow(currentToken);
                String primaryKeyId = jwtKeyRotationService.getPrimaryKeyId();

                if (!currentKeyId.equals(primaryKeyId)) {
                    response.addHeader("X-Token-Refresh-Needed", "true");
                    log.info("Token refresh needed - Current: {}, Primary: {}", currentKeyId, primaryKeyId);
                }
            }
        }
        catch (Exception e){
            throw new InvalidTokenException();
        }
    }
}
