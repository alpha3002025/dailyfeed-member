package click.dailyfeed.member.domain.jwt.service;

import click.dailyfeed.code.domain.member.key.exception.JwtKeyExpiredException;
import click.dailyfeed.code.domain.member.key.exception.PrimaryKeyMissingException;
import click.dailyfeed.code.domain.member.key.exception.PrimaryKeyNotExistException;
import click.dailyfeed.member.domain.jwt.entity.JwtKey;
import click.dailyfeed.member.domain.jwt.mapper.JwtKeyPlainMapper;
import click.dailyfeed.member.domain.jwt.repository.JwtKeyRepository;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/*
키 상태별 용도:

1. isPrimary=true, isActive=true   : 새 토큰 생성 + 검증 가능 (현재 Primary Key)
2. isPrimary=false, isActive=true  : 검증만 가능 (Grace Period 중인 이전 키들)
3. isPrimary=false, isActive=false : 완전히 비활성화 (만료된 키들)
4. isPrimary=true, isActive=false  : 불가능한 상태 (Primary는 항상 Active여야 함)

사용 시나리오:
- 토큰 생성: findPrimaryKey() 사용
- 토큰 검증: findActiveKeyByKeyId() 사용
- 키 정리: findExpiredKeys() 사용
*/

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class JwtKeyRotationService {
    private final JwtKeyRepository jwtKeyRepository;
    private final JwtKeyPlainMapper jwtKeyPlainMapper;

    @Value("${jwt.key.rotation.hours:24}")
    private int keyRotationHours;

    @Value("${jwt.key.grace.period.hours:48}")
    private int gracePeriodHours;

    @EventListener(ApplicationReadyEvent.class)
    public void init(){
        initializeKeyIfNeeded();
    }

    /**
     * 초기 키가 없는 경우 생성
     */
    private void initializeKeyIfNeeded() {
        Optional<JwtKey> primaryKey = jwtKeyRepository.findPrimaryKey();
        if (primaryKey.isEmpty()) {
            generateNewPrimaryKey();
        }
    }

    /**
     * 새로운 토큰 생성을 위한 Primary Key 조회
     */
    public Key getPrimaryKey() {
        Optional<JwtKey> primaryKey = jwtKeyRepository.findPrimaryKey();
        if (primaryKey.isEmpty()) {
            throw new PrimaryKeyMissingException();
        }
        return jwtKeyPlainMapper.convertToKey(primaryKey.get());
    }

    /**
     * Key ID로 특정 키 조회 (토큰 검증용)
     */
    public Key getKeyByKeyId(String keyId) {
        Optional<JwtKey> jwtKey = jwtKeyRepository.findActiveKeyByKeyId(keyId);
        if (jwtKey.isEmpty()) {
            throw new JwtKeyExpiredException("Key not found or expired: " + keyId);
        }
        return jwtKeyPlainMapper.convertToKey(jwtKey.get());
    }

    /**
     * 현재 Primary Key의 Key ID 반환
     */
    public String getPrimaryKeyId() {
        Optional<JwtKey> primaryKey = jwtKeyRepository.findPrimaryKey();
        if (primaryKey.isEmpty()) {
            throw new PrimaryKeyNotExistException();
        }
        return primaryKey.get().getKeyId();
    }

    /**
     * 주기적으로 키 로테이션 수행 (매 시간마다 체크)
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void rotateKeysIfNeeded() {
        Optional<JwtKey> currentPrimary = jwtKeyRepository.findPrimaryKey();

        if (currentPrimary.isEmpty()) {
            generateNewPrimaryKey();
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime keyCreatedAt = currentPrimary.get().getCreatedAt();

        // 현재 Primary Key가 KEY_ROTATION_HOURS 이상 지난 경우 새 키 생성
        if (keyCreatedAt.plusHours(keyRotationHours).isBefore(now)) {
            generateNewPrimaryKey();
        }

        // 만료된 키들 정리
        cleanupExpiredKeys();
    }

    /**
     * 새로운 Primary Key 생성
     *
     * Primary Key 교체 과정:
     * 1. 기존 Primary Key(isPrimary=true)를 일반 키(isPrimary=false)로 변경
     * 2. 새로운 키를 생성하고 Primary로 설정(isPrimary=true)
     * 3. 이후 모든 새로운 JWT 토큰은 새 Primary Key로 생성됨
     * 4. 기존 토큰들은 여전히 이전 키들로 검증 가능 (Grace Period 동안)
     */
    public void generateNewPrimaryKey() {
        // 1. 기존 Primary Key를 일반 키로 변경 (isPrimary: true -> false)
        Optional<JwtKey> existingPrimary = jwtKeyRepository.findPrimaryKey();
        if (existingPrimary.isPresent()) {
            JwtKey existing = existingPrimary.get();
            existing.disablePrimaryKey(); // 더 이상 새 토큰 생성에 사용되지 않음
            // 하지만 isActive=true 인 기존 만료 토큰 검증은 가능
            jwtKeyRepository.save(existing);
            log.info("Demoted existing primary key: {}", existing.getKeyId());
        }

        // 랜덤 Key 생성
        SecretKey secretKey = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        // 2. 새로운 Primary Key 생성 (isPrimary=true)
        JwtKey newKey = JwtKey.newKey(encodedKey, keyRotationHours, gracePeriodHours);
        jwtKeyRepository.save(newKey);

        log.info("New primary key generated with ID: {}", newKey.getKeyId());
    }

    /**
     * 만료된 키들 정리
     */
    public void cleanupExpiredKeys() {
        LocalDateTime now = LocalDateTime.now();
        List<JwtKey> expiredKeys = jwtKeyRepository.findExpiredKeys(now);

        for (JwtKey expiredKey : expiredKeys) {
            expiredKey.deactivate();
        }

        if (!expiredKeys.isEmpty()) {
            jwtKeyRepository.saveAll(expiredKeys);
            log.info("Cleaned up {}  expired keys", expiredKeys.size());
        }
    }

    /**
     * 모든 활성 키 조회 (디버깅 및 모니터링용)
     */
    public List<JwtKey> getAllActiveKeys() {
        return jwtKeyRepository.findAllActiveKeys();
    }
}
