package click.dailyfeed.member.domain.jwt.repository.jpa;

import click.dailyfeed.member.domain.jwt.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 유효한 리프레시 토큰 조회
     */
    Optional<RefreshToken> findByTokenValueAndIsRevokedFalse(String tokenValue);

    /**
     * 액세스 토큰 ID로 연관된 리프레시 토큰 조회
     */
    Optional<RefreshToken> findByAccessTokenIdAndIsRevokedFalse(String accessTokenId);

    /**
     * 사용자의 모든 유효한 리프레시 토큰 조회
     */
    List<RefreshToken> findByMemberIdAndIsRevokedFalse(Long memberId);

    /**
     * 사용자의 모든 리프레시 토큰 무효화
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true " +
            "WHERE rt.memberId = :memberId AND rt.isRevoked = false")
    int revokeAllByMemberId(@Param("memberId") Long memberId);

    /**
     * 만료된 토큰 삭제
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 무효화되고 일정 시간이 지난 토큰 삭제
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt " +
            "WHERE rt.isRevoked = true AND rt.updatedAt < :cutoffTime")
    int deleteRevokedTokensOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 특정 디바이스의 토큰 조회
     */
    Optional<RefreshToken> findByMemberIdAndDeviceInfoAndIsRevokedFalse(
            Long memberId,
            String deviceInfo
    );
}