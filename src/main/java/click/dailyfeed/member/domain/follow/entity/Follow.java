package click.dailyfeed.member.domain.follow.entity;

import click.dailyfeed.member.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Getter
@Entity
@Table(name = "member_follows")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "FOLLOWER_ID")
    private Member follower;

    @ManyToOne
    @JoinColumn(name = "FOLLOWING_ID")
    private Member following;

    @Builder
    public Follow(Member follower, Member following) {
        this.follower = follower;
        this.following = following;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Follow follow = (Follow) o;

        // 영속화되지 않은 엔티티의 경우
        if (follower == null || following == null ||
                follow.follower == null || follow.following == null) {
            return false;
        }

        // 비즈니스 키인 follower와 following의 조합으로 비교
        return Objects.equals(follower.getId(), follow.getFollower().getId()) &&
                Objects.equals(following.getId(), follow.getFollowing().getId());
    }

    @Override
    public int hashCode() {
        // follower와 following이 null일 수 있으므로 안전한 hashCode
        return Objects.hash(
                follower != null ? follower.getId() : 0,
                following != null ? following.getId() : 0
        );
    }
}
