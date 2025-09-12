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
