package click.dailyfeed.member.domain.member.redis;

import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.domain.member.member.predicate.HandleExistsPredicate;
import click.dailyfeed.code.global.cache.RedisKeyConstant;
import click.dailyfeed.member.domain.follow.repository.jpa.FollowRepository;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.entity.MemberProfile;
import click.dailyfeed.member.domain.member.mapper.MemberMapper;
import click.dailyfeed.member.domain.member.mapper.MemberProfileMapper;
import click.dailyfeed.member.domain.member.repository.jpa.MemberProfileRepository;
import click.dailyfeed.member.domain.member.repository.jpa.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
@Service
public class MemberRedisService {
    private final MemberRepository memberRepository;
    private final FollowRepository followRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberMapper memberMapper;
    private final MemberProfileMapper memberProfileMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.MemberRedisService.WEB_GET_MEMBER_BY_ID, key="#memberId")
    public MemberDto.Member getMemberOrThrow(Long memberId) {
        MemberDto.Member member = memberRepository
                .findById(memberId)
                .map(memberMapper::ofMember)
                .orElseThrow(MemberNotFoundException::new);

        return member;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.MemberRedisService.WEB_GET_MEMBER_PROFILE_BY_ID, key = "#memberId")
    public MemberProfileDto.MemberProfile findMemberProfileById(Long memberId) {
        MemberProfile memberProfile = memberProfileRepository
                .findMemberProfileByMemberId(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Long followersCount = followRepository.countFollowersByMemberId(memberId);
        Long followingsCount = followRepository.countFollowingByMemberId(memberId);

        return memberProfileMapper.fromEntity(memberProfile, followersCount, followingsCount);
    }

    @Transactional(readOnly = true)
    public MemberProfileDto.MemberProfile findMemberProfileByHandle(String memberHandle) {
        MemberProfile memberProfile = memberProfileRepository
                .findByHandle(memberHandle)
                .orElseThrow(MemberNotFoundException::new);

        Long memberId = memberProfile.getMember().getId();

        Long followersCount = followRepository.countFollowersByMemberId(memberId);
        Long followingsCount = followRepository.countFollowingByMemberId(memberId);

        return memberProfileMapper.fromEntity(memberProfile, followersCount, followingsCount);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.MemberRedisService.WEB_GET_MEMBER_PROFILE_SUMMARY_BY_ID, key = "#memberId")
    public MemberProfileDto.Summary findMemberSummaryById(Long memberId) {
        MemberProfile memberProfile = memberProfileRepository
                .findMemberProfileByMemberId(memberId)
                .orElseThrow(MemberNotFoundException::new);

        return memberProfileMapper.fromEntityToSummary(memberProfile);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.MemberRedisService.INTERNAL_LIST_MEMBERS_BY_IDS_IN, key = "#ids")
    public List<MemberProfileDto.Summary> findMembersByIdsIn(List<Long> ids) {
        List<MemberProfile> memberProfiles = memberProfileRepository.findWithImagesByMemberIdsIn(ids);

        return memberProfiles.stream()
                .map(memberProfileMapper::fromEntityToSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.MemberRedisService.INTERNAL_GET_MEMBER_PROFILE_BY_HANDLE_IS_EXISTS, key = "#handle")
    public HandleExistsPredicate isHandleExists(String handle) {
        Optional<MemberProfile> memberProfile = memberProfileRepository
                .findByHandle(handle);

        if(memberProfile.isEmpty()) {
            return HandleExistsPredicate.NOT_EXISTS;
        }
        return HandleExistsPredicate.EXISTS;
    }
}
