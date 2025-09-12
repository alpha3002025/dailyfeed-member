package click.dailyfeed.member.domain.follow.service;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.global.web.page.DailyfeedPageable;
import click.dailyfeed.code.global.web.response.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.member.domain.follow.entity.Follow;
import click.dailyfeed.member.domain.follow.mapper.FollowMapper;
import click.dailyfeed.member.domain.follow.repository.FollowRepository;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import click.dailyfeed.pagination.converter.DailyfeedPageableConverter;
import click.dailyfeed.pagination.mapper.PageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
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
    private final DailyfeedPageableConverter dailyfeedPageableConverter;
    private final PageMapper pageMapper;

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

    @Transactional(readOnly = true)
    @Cacheable(value = "follow:getMemberFollowersMore", key = "#memberId + '_from_' + #page + '_size_' + #size")
    public DailyfeedScrollResponse<DailyfeedScrollPage<FollowDto.Follower>> getMemberFollowersMore(Long memberId, int page, int size, DailyfeedPageable dailyfeedPageable) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        ///  pageable
        Pageable pageable = dailyfeedPageableConverter.convert(dailyfeedPageable);

        ///  follower
        Page<Follow> followersPage = followRepository.findFollowersByMember(member, pageable);
        List<FollowDto.Follower> followers = followersPage.getContent().stream()
                .map(Follow::getFollower)
                .map(followMapper::toFollower)
                .collect(Collectors.toList());

        return DailyfeedScrollResponse.<DailyfeedScrollPage<FollowDto.Follower>>builder()
                .content(pageMapper.fromJpaPage(followersPage, followers))
                .ok("Y").reason("OK").statusCode("200")
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "follow:getMemberFollowingsMore", key = "#memberId + '_from_' + #page + '_size_' + #size")
    public DailyfeedScrollResponse<DailyfeedScrollPage<FollowDto.Following>> getMemberFollowingsMore(Long memberId, int page, int size, DailyfeedPageable dailyfeedPageable) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        ///  pageable
        Pageable pageable = dailyfeedPageableConverter.convert(dailyfeedPageable);

        ///  follower
        Page<Follow> followingPage = followRepository.findFollowingByMember(member, pageable);
        List<FollowDto.Following> followers = followingPage.getContent().stream()
                .map(Follow::getFollowing)
                .map(followMapper::toFollowing)
                .collect(Collectors.toList());

        return DailyfeedScrollResponse.<DailyfeedScrollPage<FollowDto.Following>>builder()
                .content(pageMapper.fromJpaPage(followingPage, followers))
                .ok("Y").reason("OK").statusCode("200")
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "follow:getMemberFollow", key = "#memberId")
    public DailyfeedScrollResponse<FollowDto.FollowScrollPage> getMemberFollow(Long memberId, DailyfeedPageable dailyfeedPageable) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        ///  pageable
        Pageable pageable = dailyfeedPageableConverter.convert(dailyfeedPageable);

        ///  follower
        Page<Follow> followersPage = followRepository.findFollowersByMember(member, pageable);
        List<FollowDto.Follower> followers = followersPage.getContent().stream()
                .map(Follow::getFollower)
                .map(followMapper::toFollower)
                .collect(Collectors.toList());

        DailyfeedScrollPage<FollowDto.Follower> followerPage = pageMapper.fromJpaPage(followersPage, followers);


        ///  following
        Page<Follow> followingsPage = followRepository.findFollowingByMember(member, pageable);
        List<FollowDto.Following> followings = followersPage.getContent().stream()
                .map(Follow::getFollowing)
                .map(followMapper::toFollowing)
                .collect(Collectors.toList());

        DailyfeedScrollPage<FollowDto.Following> followingPage = pageMapper.fromJpaPage(followingsPage, followings);

        FollowDto.FollowScrollPage followScrollPage = FollowDto.FollowScrollPage.builder()
                .followers(followerPage)
                .followings(followingPage)
                .build();

        return DailyfeedScrollResponse.<FollowDto.FollowScrollPage>builder()
                .content(followScrollPage)
                .ok("Y").reason("OK").statusCode("200")
                .build();
    }
}
