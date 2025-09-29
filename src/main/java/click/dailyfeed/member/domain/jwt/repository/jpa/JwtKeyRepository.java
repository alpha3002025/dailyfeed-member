package click.dailyfeed.member.domain.jwt.repository.jpa;

import click.dailyfeed.member.domain.jwt.entity.JwtKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JwtKeyRepository extends JpaRepository<JwtKey, Long> {

    // 새로운 JWT 생성용 키 조회 (isPrimary=true AND isActive=true)
    @Query("SELECT k FROM JwtKey k WHERE k.isPrimary = true AND k.isActive = true")
    Optional<JwtKey> findPrimaryKey();

    // 기존 JWT 검증용 키 조회 (isActive=true, isPrimary 무관)
    @Query("SELECT k FROM JwtKey k WHERE k.keyId = :keyId AND k.isActive = true")
    Optional<JwtKey> findActiveKeyByKeyId(String keyId);

    // 모든 검증 가능한 키들 조회 (Grace Period 체크 포함)
    @Query("SELECT k FROM JwtKey k WHERE k.isActive = true AND k.expiresAt > :now")
    List<JwtKey> findAllActiveKeys();

    // Grace Period 만료된 키들 조회 (isActive를 false로 변경할 대상)
    @Query("SELECT j FROM JwtKey j WHERE j.expiresAt < :now")
    List<JwtKey> findExpiredKeys(LocalDateTime now);

//    Optional<JwtKey> findByKeyIdAndIsActiveTrue(String keyId);
//
//    @Query("SELECT j FROM JwtKey j WHERE j.isActive = true ORDER BY j.createdDate DESC LIMIT 1")
//    Optional<JwtKey> findLatestActiveKey();
//
//    @Modifying
//    @Query("UPDATE JwtKey j SET j.isActive = false WHERE j.expiresAt < :now")
//    int deactivateExpiredKeys(LocalDateTime now);
}
