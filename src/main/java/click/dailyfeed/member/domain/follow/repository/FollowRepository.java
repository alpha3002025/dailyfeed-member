package click.dailyfeed.member.domain.follow.repository;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.member.domain.follow.entity.Follow;
import click.dailyfeed.member.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    @Query("SELECT mf FROM Follow mf WHERE mf.follower = :following and mf.following = :follower")
    Optional<Follow> findByFollowerAndFollowing(
            @Param("follower") Member follower,
            @Param("following") Member following
    );

    // 특정 사용자의 팔로워 목록 조회
    @Query("SELECT mf FROM Follow mf WHERE mf.following = :leader")
    List<Follow> findFollowersByMember(@Param("leader") Member leader);

    // 특정 사용자가 팔로우 하는 목록 조회
    @Query("SELECT mf FROM Follow mf WHERE mf.follower = :member")
    List<Follow> findFollowingByMember(@Param("member") Member member);

    // 팔로우 관계 존재 여부 확인
    boolean existsByFollowerAndFollowing(Member follower, Member following);

    // 팔로우 관계 삭제
    void deleteByFollowerAndFollowing(Member follower, Member following);

    // 팔로워 수 카운트
    @Query("SELECT COUNT(mf) FROM Follow mf WHERE mf.following = :leader")
    Long countFollowersByMember(@Param("leader") Member leader);

    // 팔로잉 수 카운트
    @Query("SELECT COUNT(mf) FROM Follow mf WHERE mf.follower = :member")
    Long countFollowingByMember(@Param("member") Member member);

    // TODO (삭제) timeline+contents 서비스로 이관
//    @Query(value = """
//        WITH ranked_activities AS (
//            SELECT pla.member_id,
//                   pla.post_id,
//                   pla.updated_at,
//                   ROW_NUMBER() OVER (
//                       PARTITION BY pla.member_id
//                       ORDER BY pla.updated_at DESC
//                   ) as rn
//            FROM post_latest_activity pla
//            WHERE pla.activity_type != 'DELETE'
//        ),
//        latest_activities AS (
//            SELECT member_id, post_id, updated_at as latest_activity
//            FROM ranked_activities
//            WHERE rn = 1
//        )
//        SELECT new click.dailyfeed.dto.FollowingActivityDto(mf.following_id, la.post_id)
//        FROM follow mf
//        INNER JOIN latest_activities la ON mf.following_id = la.member_id
//        WHERE mf.follower_id = :memberId
//        ORDER BY la.latest_activity DESC
//        """ ,nativeQuery = true)
//    Page<FollowDto.LatestPost> findActiveFollowingMemberIds(@Param("memberId") Long memberId, Pageable pageable);
//
//    // 쿼리도 수정해야 하는데, 생성과 삭제를 2분동안 했는데 그 시간 내에 조회할 경우 1건의 행으로 도출되야 하므로
//    // distinct following_id
//    // 그 시간 내에 일단 top 30명의 활동 내역만 필요
//
//    // DTO 프로젝션을 사용하는 버전 (권장)
//    @Query("SELECT click.dailyfeed.code.domain.member.follow.dto.FollowDto.FollowActivityDto.of(" +
//            "mf.following.id, mf.following.name, pla.postId, pla.activityType, pla.lastModifiedDate) " +
//            "FROM Follow mf " +
//            "INNER JOIN PostLatestActivity pla ON mf.following.id = pla.memberId " +
//            "WHERE mf.follower.id = :memberId " +
//            "AND pla.activityType != click.dailyfeed.code.domain.feed.post.type.PostActivityType.DELETE " +
//            "ORDER BY pla.lastModifiedDate DESC")
//    Page<FollowDto.FollowActivityDto> findRecentActivitiesFromFollowing(@Param("memberId") Long memberId, Pageable pageable);
}
