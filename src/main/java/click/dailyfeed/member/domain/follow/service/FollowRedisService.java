package click.dailyfeed.member.domain.follow.service;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.member.domain.follow.entity.Follow;
import click.dailyfeed.member.domain.follow.mapper.FollowMapper;
import click.dailyfeed.member.domain.follow.repository.FollowRepository;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Service
public class FollowRedisService {
    private final MemberRepository memberRepository;
    private final FollowRepository followRepository;
    private final FollowMapper followMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "follow:getFollowingMembers", key = "#memberId")
    public List<FollowDto.Following> getFollowingMembers(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        List<Follow> followings = followRepository.findFollowingByMember(member);

        return followings.stream()
                .map(Follow::getFollowing)
                .map(followMapper::toFollowing)
                .collect(Collectors.toList());
    }

    // ✅ todo 페이징, 카운트 처리
    @Transactional(readOnly = true)
    @Cacheable(value = "follow:getMemberFollow", key = "#memberId")
    public FollowDto.FollowPage getMemberFollow(Long memberId, Pageable pageable) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        List<FollowDto.Follower> followers = followRepository.findFollowersByMember(member)
                .stream()
                .map(Follow::getFollower)
                .map(followMapper::toFollower)
                .collect(Collectors.toList());

        List<FollowDto.Following> followings = followRepository.findFollowingByMember(member)
                .stream()
                .map(Follow::getFollowing)
                .map(followMapper::toFollowing)
                .collect(Collectors.toList());

        return followMapper.ofFollow(followers, followings, pageable);
    }
}
