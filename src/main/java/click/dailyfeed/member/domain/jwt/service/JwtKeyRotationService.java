package click.dailyfeed.member.domain.jwt.service;

import click.dailyfeed.code.domain.member.key.exception.JwtKeyExpiredException;
import click.dailyfeed.code.domain.member.key.exception.PrimaryKeyMissingException;
import click.dailyfeed.code.domain.member.key.exception.PrimaryKeyNotExistException;
import click.dailyfeed.member.domain.jwt.entity.JwtKey;
import click.dailyfeed.member.domain.jwt.mapper.JwtKeyPlainMapper;
import click.dailyfeed.member.domain.jwt.repository.jpa.JwtKeyRepository;
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

//    @EventListener(ApplicationReadyEvent.class)
    public void init(){
        log.info("🔑 Initializing JWT Key Rotation Service...");

        // 먼저 중복된 Primary Key 정리
        fixDuplicatePrimaryKeys();

        // 그 다음 초기화 (재시도 로직 포함)
        initializeKeyIfNeededWithRetry();

        log.info("✅ JWT Key Rotation Service initialized successfully");
    }

    /**
     * 재시도 로직을 포함한 키 초기화
     * DB 연결 지연 등의 이슈를 대비하여 최대 3회 재시도
     *
     * 개선 사항:
     * 1. DB 연결 지연 시 재시도
     * 2. 상세한 로깅으로 문제 진단 용이
     * 3. 마지막 재시도 실패 시 명확한 에러 메시지
     */
    private void initializeKeyIfNeededWithRetry() {
        int maxRetries = 3;
        int retryDelayMs = 1000; // 1초

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Checking for existing primary key (attempt {}/{})", attempt, maxRetries);

                // DB에서 Primary Key 조회
                Optional<JwtKey> primaryKey = jwtKeyRepository.findPrimaryKey();

                if (primaryKey.isEmpty()) {
                    log.info("⚠️ No primary key found in database, generating new one (attempt {}/{})",
                             attempt, maxRetries);
                    generateNewPrimaryKey();
                    log.info("✅ New primary key generated successfully");
                    return; // 성공 - 종료
                } else {
                    // 기존 키 발견
                    JwtKey key = primaryKey.get();
                    log.info("✅ Found existing primary key: {} (created at: {}, expires at: {}) (attempt {}/{})",
                             key.getKeyId(),
                             key.getCreatedAt(),
                             key.getExpiresAt(),
                             attempt,
                             maxRetries);

                    // 키 만료 임박 경고
                    if (key.getExpiresAt() != null) {
                        LocalDateTime now = LocalDateTime.now();
                        long hoursUntilExpiry = java.time.Duration.between(now, key.getExpiresAt()).toHours();

                        if (hoursUntilExpiry <= 24) {
                            log.warn("⚠️ Primary key {} is expiring in {} hours!",
                                     key.getKeyId(), hoursUntilExpiry);
                        }
                    }

                    return; // 성공 - 종료
                }

            } catch (Exception e) {
                log.warn("⚠️ Failed to initialize JWT key (attempt {}/{}): {} - {}",
                         attempt, maxRetries, e.getClass().getSimpleName(), e.getMessage());

                if (attempt >= maxRetries) {
                    log.error("❌ Failed to initialize JWT key after {} attempts. Application may not work correctly!",
                             maxRetries, e);
                    throw new RuntimeException("JWT key initialization failed after " + maxRetries + " attempts", e);
                }

                // 재시도 전 대기
                try {
                    log.debug("Waiting {}ms before retry...", retryDelayMs);
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("❌ JWT key initialization interrupted", ie);
                    throw new RuntimeException("JWT key initialization interrupted", ie);
                }
            }
        }
    }

    /**
     * 중복된 Primary Key 정리
     * 데이터 정합성 문제로 isPrimary=true인 키가 여러 개 존재할 경우,
     * 가장 최신 키만 Primary로 유지하고 나머지는 일반 키로 변경
     */
    public void fixDuplicatePrimaryKeys() {
        List<JwtKey> primaryKeys = jwtKeyRepository.findAllPrimaryKeys();

        if (primaryKeys.size() > 1) {
            log.warn("⚠️ Found {} primary keys, fixing duplicate primary keys...", primaryKeys.size());

            // 가장 최신 키(createdAt 기준 내림차순 정렬 후 첫 번째)를 제외하고 나머지 disablePrimaryKey
            primaryKeys.stream()
                    .sorted((k1, k2) -> k2.getCreatedAt().compareTo(k1.getCreatedAt())) // 최신순 정렬
                    .skip(1) // 첫 번째(최신) 제외
                    .forEach(key -> {
                        log.warn("Demoting duplicate primary key: {} (created at: {})",
                                key.getKeyId(), key.getCreatedAt());
                        key.disablePrimaryKey();
                        jwtKeyRepository.save(key);
                    });

            log.info("✅ Fixed duplicate primary keys, kept the latest key as primary");
        } else if (primaryKeys.size() == 1) {
            log.debug("✅ Primary key status is healthy (1 primary key found)");
        } else {
            log.debug("No primary key found yet, will generate new one");
        }
    }

    /**
     * 초기 키가 없는 경우 생성
     * @deprecated Use initializeKeyIfNeededWithRetry() instead for better reliability
     */
    @Deprecated
    public void initializeKeyIfNeeded() {
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
            log.error("❌ No primary key available for token generation");
            throw new PrimaryKeyMissingException();
        }
        return jwtKeyPlainMapper.convertToKey(primaryKey.get());
    }

    /**
     * Key ID로 특정 키 조회 (토큰 검증용)
     */
    public Key getKeyByKeyId(String keyId) {
        log.debug("Looking up JWT key with keyId: {}", keyId);
        Optional<JwtKey> jwtKey = jwtKeyRepository.findActiveKeyByKeyId(keyId);
        if (jwtKey.isEmpty()) {
            log.warn("❌ JWT key not found or expired: keyId={}", keyId);
            throw new JwtKeyExpiredException("Key not found or expired: " + keyId);
        }
        log.debug("✅ Found JWT key: keyId={}, isActive={}, isPrimary={}",
            jwtKey.get().getKeyId(), jwtKey.get().getIsActive(), jwtKey.get().getIsPrimary());
        return jwtKeyPlainMapper.convertToKey(jwtKey.get());
    }

    /**
     * 현재 Primary Key의 Key ID 반환
     */
    public String getPrimaryKeyId() {
        Optional<JwtKey> primaryKey = jwtKeyRepository.findPrimaryKey();
        if (primaryKey.isEmpty()) {
            log.error("❌ No primary key exists");
            throw new PrimaryKeyNotExistException();
        }
        return primaryKey.get().getKeyId();
    }

    /**
     * 주기적으로 키 로테이션 수행 (매 시간마다 체크)
     */
//    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void rotateKeysIfNeeded() {
        log.debug("🔄 Checking if key rotation is needed...");

        Optional<JwtKey> currentPrimary = jwtKeyRepository.findPrimaryKey();

        if (currentPrimary.isEmpty()) {
            log.warn("⚠️ No primary key found during scheduled rotation, generating new one");
            generateNewPrimaryKey();
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime keyCreatedAt = currentPrimary.get().getCreatedAt();

        // 현재 Primary Key가 KEY_ROTATION_HOURS 이상 지난 경우 새 키 생성
        if (keyCreatedAt.plusHours(keyRotationHours).isBefore(now)) {
            log.info("🔄 Key rotation triggered: current key is {} hours old (threshold: {} hours)",
                     java.time.Duration.between(keyCreatedAt, now).toHours(), keyRotationHours);
            generateNewPrimaryKey();
        } else {
            log.debug("✅ Current key is still valid (created {} hours ago, rotation at {} hours)",
                     java.time.Duration.between(keyCreatedAt, now).toHours(), keyRotationHours);
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
        log.info("🔑 Generating new primary key...");

        // 1. 모든 기존 Primary Key들을 일반 키로 변경 (isPrimary: true -> false)
        // 중복 방지를 위해 findAllPrimaryKeys() 사용
        List<JwtKey> existingPrimaryKeys = jwtKeyRepository.findAllPrimaryKeys();
        if (!existingPrimaryKeys.isEmpty()) {
            for (JwtKey existing : existingPrimaryKeys) {
                existing.disablePrimaryKey(); // 더 이상 새 토큰 생성에 사용되지 않음
                // 하지만 isActive=true인 경우 기존 토큰 검증은 가능
                jwtKeyRepository.save(existing);
                log.info("Demoted existing primary key: {} to regular key", existing.getKeyId());
            }
        }

        // 랜덤 Key 생성
        SecretKey secretKey = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        // 2. 새로운 Primary Key 생성 (isPrimary=true)
        JwtKey newKey = JwtKey.newKey(encodedKey, keyRotationHours, gracePeriodHours);
        jwtKeyRepository.save(newKey);

        log.info("✅ New primary key generated with ID: {} (will expire at: {})",
                 newKey.getKeyId(), newKey.getExpiresAt());
    }

    /**
     * 만료된 키들 정리
     */
    public void cleanupExpiredKeys() {
        LocalDateTime now = LocalDateTime.now();
        List<JwtKey> expiredKeys = jwtKeyRepository.findExpiredKeys(now);

        if (!expiredKeys.isEmpty()) {
            for (JwtKey expiredKey : expiredKeys) {
                expiredKey.deactivate();
                log.info("Deactivated expired key: {} (expired at: {})",
                         expiredKey.getKeyId(), expiredKey.getExpiresAt());
            }

            jwtKeyRepository.saveAll(expiredKeys);
            log.info("✅ Cleaned up {} expired keys", expiredKeys.size());
        } else {
            log.debug("✅ No expired keys to clean up");
        }
    }

    /**
     * 모든 활성 키 조회 (디버깅 및 모니터링용)
     */
    public List<JwtKey> getAllActiveKeys() {
        List<JwtKey> activeKeys = jwtKeyRepository.findAllActiveKeys();
        log.debug("Found {} active keys", activeKeys.size());
        return activeKeys;
    }
}
