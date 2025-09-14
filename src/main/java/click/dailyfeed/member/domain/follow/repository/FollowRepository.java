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

    // timeline 내에서 팔로잉 멤버들의 피드 조회를 위한 팔로잉멤버 목록 조회
    @Query("SELECT mf FROM Follow mf WHERE mf.follower = :member")
    List<Follow> findFollowingByMember(Member member);

    // 팔로잉 조회 (내가 팔로우하는 사람들)
    @Query("SELECT f.following.id " +
            "FROM Follow f " +
            "INNER JOIN f.following following " +
            "INNER JOIN Member followingMember ON followingMember.id = following.id " +
            "WHERE f.follower = :member")
    Page<Follow> findFollowingsByMember(@Param("member") Member member, Pageable pageable);

    // 팔로잉 조회 (내가 팔로우하는 사람들)
    @Query("SELECT f.following.id " +
            "FROM Follow f " +
            "INNER JOIN f.following following " +
            "INNER JOIN Member followingMember ON followingMember.id = following.id " +
            "WHERE f.follower = :member")
    Page<Long> findFollowingsIdByMember(@Param("member") Member member, Pageable pageable);

    // id 기반 조회 (아직 결정을 못함)
    @Query("SELECT f.following.id " +
            "FROM Follow f " +
            "INNER JOIN f.following following " +
            "INNER JOIN Member followingMember ON followingMember.id = following.id " +
            "WHERE f.follower.id = :memberId")
    Page<Long> findFollowingsIdByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    // 팔로잉 조회 (내가 팔로우하는 사람들) - 피드 조회 용도
    @Query("SELECT f.following.id " +
            "FROM Follow f " +
            "INNER JOIN f.following following " +
            "INNER JOIN Member followingMember ON followingMember.id = following.id " +
            "WHERE f.follower = :member")
    List<Long> findFollowingsIdByMember(@Param("member") Member member);

    // 팔로잉 조회 (내가 팔로우하는 사람들) - 피드 조회 용도
    @Query("SELECT f.following.id " +
            "FROM Follow f " +
            "INNER JOIN f.following following " +
            "INNER JOIN Member followingMember ON followingMember.id = following.id " +
            "WHERE f.follower.id = :memberId")
    List<Long> findFollowingsIdByMemberId(@Param("memberId") Long memberId);


    // 팔로워 조회 (나를 팔로우하는 사람들)
    @Query("SELECT f " +
            "FROM Follow f " +
            "INNER JOIN f.follower follower " +
            "WHERE f.following = :member")
    Page<Follow> findFollowersByMember(@Param("member") Member member, Pageable pageable);


    @Query("SELECT f.follower.id " +
            "FROM Follow f " +
            "INNER JOIN f.follower follower " +
            "WHERE f.following = :member")
    Page<Long> findFollowersIdByMember(@Param("member") Member member, Pageable pageable);

    // id 기반 조회
    // 팔로워 조회 (나를 팔로우하는 사람들)
    @Query("SELECT f.follower.id " +
            "FROM Follow f " +
            "INNER JOIN f.follower follower " +
            "WHERE f.following.id = :memberId")
    Page<Long> findFollowersIdByMemberId(@Param("memberId") Long memberId, Pageable pageable);


    // 팔로워 리스트 조회 금지!!!
//    @Query("SELECT f.follower.id " +
//            "FROM Follow f " +
//            "INNER JOIN f.follower follower " +
//            "INNER JOIN Member followerMember ON followerMember.id = follower.id " +
//            "WHERE f.following = :member")
//    List<Long> findFollowersIdByMember(@Param("member") Member member);

    // 특정 사용자가 팔로우 하는 목록 조회 (페이징)
    @Query("SELECT mf FROM Follow mf WHERE mf.follower = :member")
    List<Follow> findFollowingByMemberPaging(@Param("member") Member member, Pageable pageable);

    // 팔로우 관계 존재 여부 확인
    boolean existsByFollowerAndFollowing(Member follower, Member following);

    // 팔로우 관계 삭제
    void deleteByFollowerAndFollowing(Member follower, Member following);

    // 팔로워 수 카운트
    @Query("SELECT COUNT(mf) FROM Follow mf WHERE mf.following.id = :memberId")
    Long countFollowersByMemberId(@Param("memberId") Long memberId);

    // 팔로잉 수 카운트
    @Query("SELECT COUNT(mf) FROM Follow mf WHERE mf.follower.id = :memberId")
    Long countFollowingByMemberId(@Param("memberId") Long memberId);
}
