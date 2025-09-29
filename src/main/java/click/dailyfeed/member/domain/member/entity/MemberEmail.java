package click.dailyfeed.member.domain.member.entity;

import click.dailyfeed.member.domain.base.BaseTimeCreatedAtEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "member_emails",
    indexes = {
        @Index(name = "idx_active_verified", columnList = "is_active, verified, email"),
        @Index(name = "idx_cleanup", columnList = "is_active, created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_user_active", columnNames = {"member_id", "is_active"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "ofAll")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class MemberEmail extends BaseTimeCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(nullable = false)
    private Boolean verified = false;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Builder(builderMethodName = "newMember", builderClassName = "NewMember")
    public MemberEmail(Member member, String email) {
        this.member = member;
        this.email = email;
        this.isActive = true;
        this.verified = false;
    }

    public void activate() {
        this.isActive = true;
        this.activatedAt = LocalDateTime.now();
        this.deactivatedAt = null;
    }

    public void deactivate() {
        this.isActive = false;
        this.deactivatedAt = LocalDateTime.now();
    }

    public void verify() {
        this.verified = true;
    }

    public void updateEmail(String email) {
        this.email = email;
        this.verified = false;
    }
}