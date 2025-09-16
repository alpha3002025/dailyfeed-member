package click.dailyfeed.member.domain.jwt.entity;

import click.dailyfeed.member.domain.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "jwt_refresh_tokens",
//    schema = "dailyfeed",
    indexes = {
        @Index(name = "idx_member_id", columnList = "member_id"),
        @Index(name = "idx_access_token_id", columnList = "access_token_id"),
        @Index(name = "idx_expires_at", columnList = "expires_at"),
        @Index(name = "idx_token_value", columnList = "token_value")
    }
)
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "token_id", unique = true, nullable = false)
    private String tokenId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "token_value", nullable = false, length = 512, unique = true)
    private String tokenValue;

    @Column(name = "access_token_id", nullable = false)
    private String accessTokenId;  // JTI of associated access token

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * 팩토리 메서드 - 모든 값을 외부에서 주입받음
     */
    public static RefreshToken create(
            String tokenId,
            Long memberId,
            String tokenValue,
            String accessTokenId,
            LocalDateTime expiresAt,
            String deviceInfo,
            String ipAddress) {

        return RefreshToken.builder()
                .tokenId(tokenId)
                .memberId(memberId)
                .tokenValue(tokenValue)
                .accessTokenId(accessTokenId)
                .expiresAt(expiresAt)
                .isRevoked(false)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();
    }

    /**
     * 토큰 무효화 - JPA 관리 상태에서만 호출
     */
    public void revoke() {
        this.isRevoked = true;
    }

    /**
     * 유효성 검증 - 순수 함수
     * @param now 검증 시점
     * @return 토큰이 유효한지 여부
     */
    public boolean isValidAt(LocalDateTime now) {
        return !isRevoked && now.isBefore(expiresAt);
    }

    /**
     * 만료 여부 확인 - 순수 함수
     * @param now 확인 시점
     * @return 토큰이 만료되었는지 여부
     */
    public boolean isExpiredAt(LocalDateTime now) {
        return now.isAfter(expiresAt) || now.isEqual(expiresAt);
    }
}
