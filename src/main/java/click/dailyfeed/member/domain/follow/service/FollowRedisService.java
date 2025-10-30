package click.dailyfeed.member.domain.follow.service;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.global.cache.RedisCacheableConstant;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.member.domain.follow.repository.jpa.FollowRepository;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.entity.MemberProfile;
import click.dailyfeed.member.domain.member.mapper.MemberProfileMapper;
import click.dailyfeed.member.domain.member.repository.jpa.MemberProfileRepository;
import click.dailyfeed.member.domain.member.repository.jpa.MemberRepository;
import click.dailyfeed.pagination.mapper.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class FollowRedisService {
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final FollowRepository followRepository;
    private final PageMapper pageMapper;
    private final MemberProfileMapper memberProfileMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = RedisCacheableConstant.FollowPrefix.API_INTERNAL_LIST_FOLLOWING_MEMBERS_BY_MEMBER_ID , key = "#memberId")
    public List<MemberProfileDto.Summary> getFollowingMembers(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        List<Long> followingIds = followRepository.findFollowingsIdByMemberId(memberId);
        List<MemberProfile> profiles = memberProfileRepository.findWithImagesByMemberIdsIn(followingIds);

        return profiles
                .stream()
                .map(memberProfileMapper::fromEntityToSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public DailyfeedScrollPage<MemberProfileDto.Summary> getMemberFollowersMore(Long memberId, Pageable pageable) {
        ///  followers
        Slice<Long> followerIds = followRepository.findFollowersIdByMemberId(memberId, pageable);

        Slice<MemberProfile> profiles = memberProfileRepository.findWithImagesByMemberIdsIn(followerIds.getContent(), pageable);

        List<MemberProfileDto.Summary> result = profiles.getContent()
                .stream()
                .map(memberProfileMapper::fromEntityToSummary)
                .toList();

        return pageMapper.fromJpaSliceToDailyfeedScrollPage(followerIds, result);
    }

    @Transactional(readOnly = true)
    public DailyfeedScrollPage<MemberProfileDto.Summary> getMemberFollowingsMore(Long memberId, Pageable pageable) {
        ///  followings
        Slice<Long> followingIds = followRepository.findFollowingsIdByMemberId(memberId, pageable);

        Slice<MemberProfile> profiles = memberProfileRepository.findWithImagesByMemberIdsIn(followingIds.getContent(), pageable);

        List<MemberProfileDto.Summary> result = profiles.getContent()
                .stream()
                .map(memberProfileMapper::fromEntityToSummary)
                .toList();

        return pageMapper.fromJpaSliceToDailyfeedScrollPage(followingIds, result);
    }

    @Transactional(readOnly = true)
    public FollowDto.FollowScrollPage getMemberFollow(Long memberId, Pageable pageable) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        ///  follower
        Slice<Long> followersId = followRepository.findFollowersIdByMember(member, pageable);
        Slice<MemberProfile> followersProfiles = memberProfileRepository.findWithImagesByMemberIdsIn(followersId.getContent(), pageable);
        List<MemberProfileDto.Summary> followers = followersProfiles.getContent().stream().map(memberProfileMapper::fromEntityToSummary).toList();
        DailyfeedScrollPage<MemberProfileDto.Summary> followersPage = pageMapper.fromJpaSliceToDailyfeedScrollPage(followersProfiles, followers);

        ///  following
        Slice<Long> followingsId = followRepository.findFollowingsIdByMember(member, pageable);
        Slice<MemberProfile> followingsProfiles = memberProfileRepository.findWithImagesByMemberIdsIn(followingsId.getContent(), pageable);
        List<MemberProfileDto.Summary> followings = followingsProfiles.getContent().stream().map(memberProfileMapper::fromEntityToSummary).toList();
        DailyfeedScrollPage<MemberProfileDto.Summary> followingsPage = pageMapper.fromJpaSliceToDailyfeedScrollPage(followingsProfiles, followings);

        return FollowDto.FollowScrollPage.builder()
                .followers(followersPage)
                .followings(followingsPage)
                .build();
    }
}
