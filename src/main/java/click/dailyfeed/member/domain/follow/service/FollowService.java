package click.dailyfeed.member.domain.follow.service;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.follow.exception.FollowRelationshipAlreadyExistsException;
import click.dailyfeed.code.domain.member.follow.exception.FollowRelationshipNotFoundException;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.global.web.response.DailyfeedPage;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.member.domain.follow.entity.Follow;
import click.dailyfeed.member.domain.follow.mapper.FollowMapper;
import click.dailyfeed.member.domain.follow.repository.FollowRepository;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class FollowService {
    private final FollowRepository followRepository;
    private final FollowMapper followMapper;
    private final MemberRepository memberRepository;

    public Boolean follow(Long memberToFollowId, Long followerId) {
        Member memberToFollow = getMemberOrThrow(memberToFollowId);
        Member follower = getMemberOrThrow(followerId);

        getFollowRelationshipOrThrowIfAlreadyExists(memberToFollow, follower);

        Follow follow = Follow.builder()
                .follower(follower)
                .following(memberToFollow)
                .build();

        follower.follow(memberToFollow, follow);

        followRepository.save(follow);
        return true;
    }

    public Boolean unfollow(Long memberToUnfollowId, Long followerId) {
        Member memberToUnfollow = getMemberOrThrow(memberToUnfollowId);
        Member follower = getMemberOrThrow(followerId);
        Follow follow = getFollowRelationshipOrThrowIfNotFound(memberToUnfollow, follower);

        follower.unfollow(memberToUnfollow, follow);

        followRepository.deleteById(follow.getId());
        return Boolean.TRUE;
    }

    @Transactional(readOnly = true)
    public FollowDto.Follow getMyFollow(Long myId) {
        List<FollowDto.Follower> followers = getFollowers(myId);
        List<FollowDto.Following> followings = getFollowings(myId);

        return followMapper.ofFollow(followers, followings);
    }

    @Transactional(readOnly = true)
    public FollowDto.Follow getMemberFollow(Long memberId) {
        List<FollowDto.Follower> followers = getFollowers(memberId);
        List<FollowDto.Following> followings = getFollowings(memberId);

        return followMapper.ofFollow(followers, followings);
    }

    @Transactional(readOnly = true)
    public List<FollowDto.Follower> getFollowers(Long memberId) {
        Member leader = getMemberOrThrow(memberId);

        List<Follow> followers = followRepository.findFollowersByMember(leader);

        return followers.stream()
                .map(Follow::getFollower)
                .map(followMapper::toFollower)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FollowDto.Following> getFollowings(Long memberId) {
        Member member = getMemberOrThrow(memberId);

        List<Follow> followings = followRepository.findFollowingByMember(member);

        return followings.stream()
                .map(Follow::getFollowing)
                .map(followMapper::toFollowing)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Boolean isFollowing(Long followerId, Long followingId){
        Member follower = getMemberOrThrow(followerId);
        Member following = getMemberOrThrow(followerId);

        return followRepository.existsByFollowerAndFollowing(follower, following);
    }

    public Member getMemberOrThrow(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    public Follow getFollowRelationshipOrThrowIfNotFound(Member follower, Member following) {
        return followRepository
                .findByFollowerAndFollowing(follower, following)
                .orElseThrow(FollowRelationshipNotFoundException::new);
    }

    public void getFollowRelationshipOrThrowIfAlreadyExists(Member follower, Member following) {
        boolean exists = followRepository.existsByFollowerAndFollowing(follower, following);
        if(exists) {
            throw new FollowRelationshipAlreadyExistsException();
        }
    }

    // TODO (삭제) timeline+contents 서비스로 이관
//    public DailyfeedPageResponse<FollowDto.LatestPost> findRecentActivitiesFromFollowing(Long memberId, Pageable pageable) {
//        Page<FollowDto.LatestPost> page = followRepository.findActiveFollowingMemberIds(memberId, pageable);
//        DailyfeedPage<FollowDto.LatestPost> dailyfeedPage = followMapper.fromPage(page);
//        return DailyfeedPageResponse.<FollowDto.LatestPost>builder()
//                .content(dailyfeedPage).ok("Y").reason("SUCCESS").statusCode("200")
//                .build();
//    }
}
