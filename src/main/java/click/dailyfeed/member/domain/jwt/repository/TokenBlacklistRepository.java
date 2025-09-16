package click.dailyfeed.member.domain.jwt.repository;

import click.dailyfeed.member.domain.jwt.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    /**
     * JTI로 블랙리스트 확인
     */
    boolean existsByJti(String jti);

    /**
     * JTI로 블랙리스트 항목 조회
     */
    Optional<TokenBlacklist> findByJti(String jti);

    /**
     * 사용자의 블랙리스트 항목 조회
     */
    List<TokenBlacklist> findByMemberId(Long memberId);

    /**
     * 만료된 블랙리스트 항목 삭제
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 만료 후 일정 시간이 지난 항목 삭제
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb " +
            "WHERE tb.expiresAt < :cutoffTime")
    int deleteTokensExpiredBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}