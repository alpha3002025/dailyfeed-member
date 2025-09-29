package click.dailyfeed.member.domain.follow.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FollowingCountProjection {
    private Long toMemberId;
    private Integer followingCount;
}