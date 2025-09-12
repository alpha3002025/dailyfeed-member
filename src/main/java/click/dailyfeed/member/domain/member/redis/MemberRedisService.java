package click.dailyfeed.member.domain.member.redis;

import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.member.domain.follow.repository.FollowRepository;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.mapper.MemberMapper;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Service
public class MemberRedisService {
    private final MemberRepository memberRepository;
    private final FollowRepository followRepository;
    private final MemberMapper memberMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "members:getMemberOrThrow", key="#memberId")
    public MemberDto.Member getMemberOrThrow(Long memberId) {
        return memberRepository
                .findById(memberId)
                .map(memberMapper::ofMember)
                .orElseThrow(MemberNotFoundException::new);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "members:findMemberProfileById", key = "#memberId")
    public MemberDto.MemberProfile findMemberProfileById(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Long followersCount = followRepository.countFollowersByMember(member);
        Long followingsCount = followRepository.countFollowingByMember(member);

        return MemberDto.MemberProfile.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .followerCount(followersCount)
                .followingCount(followingsCount)
                .build();
    }

    // ✅ TODO 타임라인 서비스 측 feign 측에도 네임스페이스를 다르게 한 캐싱추가해야 함 (e.g. timeline:findMemberByIds)
    @Transactional(readOnly = true)
    @Cacheable(value = "member:findMembersByIds", key = "#ids")
    public List<MemberDto.Member> findMembersByIds(List<Long> ids) {
        return memberRepository.findByIdIn(ids).stream()
                .map(memberMapper::ofMember)
                .collect(Collectors.toList());
    }

}
