package click.dailyfeed.member.domain.follow.repository;

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
    Page<Follow> findFollowersByMember(@Param("leader") Member leader, Pageable pageable);

    @Query("SELECT mf FROM Follow mf WHERE mf.follower = :member")
    Page<Follow> findFollowingByMember(Member member, Pageable pageable);

    // timeline 내에서 팔로잉 멤버들의 피드 조회를 위한 팔로잉멤버 목록 조회
    @Query("SELECT mf FROM Follow mf WHERE mf.follower = :member")
    List<Follow> findFollowingByMember(Member member);

    // 특정 사용자가 팔로우 하는 목록 조회 (페이징)
    @Query("SELECT mf FROM Follow mf WHERE mf.follower = :member")
    List<Follow> findFollowingByMemberPaging(@Param("member") Member member, Pageable pageable);

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
}
