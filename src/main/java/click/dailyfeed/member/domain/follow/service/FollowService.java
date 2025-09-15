package click.dailyfeed.member.domain.follow.service;

import click.dailyfeed.code.domain.member.follow.exception.FollowRelationshipAlreadyExistsException;
import click.dailyfeed.code.domain.member.follow.exception.FollowRelationshipNotFoundException;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.member.domain.follow.entity.Follow;
import click.dailyfeed.member.domain.follow.repository.FollowRepository;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class FollowService {
    private final FollowRepository followRepository;
    private final MemberRepository memberRepository;

    /**
     * "followerId 가 memberToFollowId 를 follow 한다"
     * @param followerId 팔로우 요청한 멤버 id
     * @param memberToFollowId 팔로우 하려는 멤버 id
     * @return
     */
    public Boolean follow(Long followerId, Long memberToFollowId) {
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

    /**
     * "followerId 가 memberToUnfollowId 를 unfollow 한다"
     * @param followerId 언팔로우 요청한 멤버 id
     * @param memberToUnfollowId 언팔로우 하려는 멤버 id
     * @return
     */
    public Boolean unfollow(Long followerId, Long memberToUnfollowId) {
        Member memberToUnfollow = getMemberOrThrow(memberToUnfollowId);
        Member follower = getMemberOrThrow(followerId);
        Follow follow = getFollowRelationshipOrThrowIfNotFound(memberToUnfollow, follower);

        follower.unfollow(memberToUnfollow, follow);

        followRepository.deleteById(follow.getId());
        return Boolean.TRUE;
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
}
