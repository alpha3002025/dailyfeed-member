package click.dailyfeed.member.domain.follow.service;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.global.cache.RedisKeyConstant;
import click.dailyfeed.code.global.web.page.DailyfeedPageable;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.member.domain.follow.mapper.FollowMapper;
import click.dailyfeed.member.domain.follow.repository.FollowRepository;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.entity.MemberProfile;
import click.dailyfeed.member.domain.member.mapper.MemberProfileMapper;
import click.dailyfeed.member.domain.member.repository.MemberProfileRepository;
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

@Transactional
@RequiredArgsConstructor
@Service
public class FollowRedisService {
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final FollowRepository followRepository;
    private final FollowMapper followMapper;
    private final DailyfeedPageableConverter dailyfeedPageableConverter;
    private final PageMapper pageMapper;
    private final MemberProfileMapper memberProfileMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.FollowRedisService.INTERNAL_LIST_FOLLOWING_MEMBERS_BY_MEMBER_ID , key = "#memberId")
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
    @Cacheable(value = RedisKeyConstant.FollowRedisService.WEB_PAGE_FOLLOWERS_MORE_BY_MEMBER_ID, key = "#memberId + '_from_' + #page + '_size_' + #size")
    public DailyfeedScrollPage<MemberProfileDto.Summary> getMemberFollowersMore(Long memberId, int page, int size, DailyfeedPageable dailyfeedPageable) {
        ///  pageable
        Pageable pageable = dailyfeedPageableConverter.convert(dailyfeedPageable);

        ///  followers
        Page<Long> followerIds = followRepository.findFollowersIdByMemberId(memberId, pageable);

        Page<MemberProfile> profiles = memberProfileRepository.findWithImagesByMemberIdsIn(followerIds.getContent(), pageable);

        List<MemberProfileDto.Summary> result = profiles.getContent()
                .stream()
                .map(memberProfileMapper::fromEntityToSummary)
                .toList();

        return pageMapper.fromJpaPageToDailyfeedScrollPage(followerIds, result);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.FollowRedisService.WEB_PAGE_FOLLOWINGS_MORE_BY_MEMBER_ID, key = "#memberId + '_from_' + #page + '_size_' + #size")
    public DailyfeedScrollPage<MemberProfileDto.Summary> getMemberFollowingsMore(Long memberId, int page, int size, DailyfeedPageable dailyfeedPageable) {
        ///  pageable
        Pageable pageable = dailyfeedPageableConverter.convert(dailyfeedPageable);

        ///  followings
        Page<Long> followingIds = followRepository.findFollowingsIdByMemberId(memberId, pageable);

        Page<MemberProfile> profiles = memberProfileRepository.findWithImagesByMemberIdsIn(followingIds.getContent(), pageable);

        List<MemberProfileDto.Summary> result = profiles.getContent()
                .stream()
                .map(memberProfileMapper::fromEntityToSummary)
                .toList();

        return pageMapper.fromJpaPageToDailyfeedScrollPage(followingIds, result);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.FollowRedisService.WEB_GET_FOLLOW_BY_MEMBER_ID, key = "#memberId")
    public FollowDto.FollowScrollPage getMemberFollow(Long memberId, DailyfeedPageable dailyfeedPageable) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        ///  pageable
        Pageable pageable = dailyfeedPageableConverter.convert(dailyfeedPageable);

        ///  follower
        Page<Long> followersId = followRepository.findFollowersIdByMember(member, pageable);
        Page<MemberProfile> followersProfiles = memberProfileRepository.findWithImagesByMemberIdsIn(followersId.getContent(), pageable);
        List<MemberProfileDto.Summary> followers = followersProfiles.getContent().stream().map(memberProfileMapper::fromEntityToSummary).toList();
        DailyfeedScrollPage<MemberProfileDto.Summary> followersPage = pageMapper.fromJpaPageToDailyfeedScrollPage(followersProfiles, followers);

        ///  following
        Page<Long> followingsId = followRepository.findFollowingsIdByMember(member, pageable);
        Page<MemberProfile> followingsProfiles = memberProfileRepository.findWithImagesByMemberIdsIn(followingsId.getContent(), pageable);
        List<MemberProfileDto.Summary> followings = followingsProfiles.getContent().stream().map(memberProfileMapper::fromEntityToSummary).toList();
        DailyfeedScrollPage<MemberProfileDto.Summary> followingsPage = pageMapper.fromJpaPageToDailyfeedScrollPage(followingsProfiles, followings);

        return FollowDto.FollowScrollPage.builder()
                .followers(followersPage)
                .followings(followingsPage)
                .build();
    }
}
