package click.dailyfeed.member.domain.jwt.entity;

import click.dailyfeed.member.domain.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "jwt_blacklist",
//    schema = "dailyfeed",
    indexes = {
        @Index(name = "idx_token_jti", columnList = "jti"),
        @Index(name = "idx_expires_at", columnList = "expires_at"),
        @Index(name = "idx_member_id", columnList = "member_id")
    }
)
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class TokenBlacklist extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "jti", unique = true, nullable = false)
    private String jti;  // JWT ID from access token

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "reason", length = 100)
    private String reason;

    /**
     * 팩토리 메서드 - 모든 값을 외부에서 주입받음
     */
    public static TokenBlacklist create(
            String jti,
            Long memberId,
            LocalDateTime expiresAt,
            String reason) {

        return TokenBlacklist.builder()
                .jti(jti)
                .memberId(memberId)
                .expiresAt(expiresAt)
                .reason(reason)
                .build();
    }

    /**
     * 만료 여부 확인 - 순수 함수
     * @param now 확인 시점
     * @return 블랙리스트 항목이 만료되었는지 여부
     */
    public boolean isExpiredAt(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    /**
     * 정리 가능 여부 확인 - 순수 함수
     * @param now 확인 시점
     * @return 블랙리스트 항목을 삭제해도 되는지 여부
     */
    public boolean canBeCleanedUp(LocalDateTime now) {
        // 만료 후 24시간이 지나면 정리 가능
        return now.isAfter(expiresAt.plusHours(24));
    }
}