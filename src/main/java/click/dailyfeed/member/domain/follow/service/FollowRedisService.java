package click.dailyfeed.member.domain.follow.service;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.global.web.page.DailyfeedPageable;
import click.dailyfeed.code.global.web.response.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.member.domain.follow.entity.Follow;
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
import java.util.stream.Collectors;

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
    @Cacheable(value = "follow:getFollowingMembers", key = "#memberId")
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
    @Cacheable(value = "follow:getMemberFollowersMore", key = "#memberId + '_from_' + #page + '_size_' + #size")
    public DailyfeedScrollResponse<DailyfeedScrollPage<MemberProfileDto.Summary>> getMemberFollowersMore(Long memberId, int page, int size, DailyfeedPageable dailyfeedPageable) {
        ///  pageable
        Pageable pageable = dailyfeedPageableConverter.convert(dailyfeedPageable);

        ///  followings
        Page<Long> followerIds = followRepository.findFollowersIdByMemberId(memberId, pageable);

        Page<MemberProfile> profiles = memberProfileRepository.findWithImagesByMemberIdsIn(followerIds.getContent(), pageable);

        List<MemberProfileDto.Summary> result = profiles.getContent()
                .stream()
                .map(memberProfileMapper::fromEntityToSummary)
                .toList();

        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .content(pageMapper.fromJpaPage(followerIds, result))
                .ok("Y").reason("OK").statusCode("200")
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "follow:getMemberFollowingsMore", key = "#memberId + '_from_' + #page + '_size_' + #size")
    public DailyfeedScrollResponse<DailyfeedScrollPage<MemberProfileDto.Summary>> getMemberFollowingsMore(Long memberId, int page, int size, DailyfeedPageable dailyfeedPageable) {
        ///  pageable
        Pageable pageable = dailyfeedPageableConverter.convert(dailyfeedPageable);

        ///  followings
        Page<Long> followingIds = followRepository.findFollowingsIdByMemberId(memberId, pageable);

        Page<MemberProfile> profiles = memberProfileRepository.findWithImagesByMemberIdsIn(followingIds.getContent(), pageable);

        List<MemberProfileDto.Summary> result = profiles.getContent()
                .stream()
                .map(memberProfileMapper::fromEntityToSummary)
                .toList();

        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .content(pageMapper.fromJpaPage(followingIds, result))
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
        Page<Long> followersId = followRepository.findFollowersIdByMember(member, pageable);
        Page<MemberProfile> followersProfiles = memberProfileRepository.findWithImagesByMemberIdsIn(followersId.getContent(), pageable);
        List<MemberProfileDto.Summary> followers = followersProfiles.getContent().stream().map(memberProfileMapper::fromEntityToSummary).toList();
        DailyfeedScrollPage<MemberProfileDto.Summary> followersPage = pageMapper.fromJpaPage(followersProfiles, followers);

        ///  following
        Page<Long> followingsId = followRepository.findFollowingsIdByMember(member, pageable);
        Page<MemberProfile> followingsProfiles = memberProfileRepository.findWithImagesByMemberIdsIn(followingsId.getContent(), pageable);
        List<MemberProfileDto.Summary> followings = followingsProfiles.getContent().stream().map(memberProfileMapper::fromEntityToSummary).toList();
        DailyfeedScrollPage<MemberProfileDto.Summary> followingsPage = pageMapper.fromJpaPage(followingsProfiles, followings);

        return DailyfeedScrollResponse.<FollowDto.FollowScrollPage>builder()
                .content(
                    FollowDto.FollowScrollPage.builder()
                            .followers(followersPage)
                            .followings(followingsPage)
                            .build()
                )
                .ok("Y").reason("OK").statusCode("200")
                .build();
    }
}
