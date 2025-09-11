package click.dailyfeed.member.domain.member.service;

import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.member.domain.follow.repository.FollowRepository;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.mapper.MemberMapper;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberMapper memberMapper;
    private final MemberRepository memberRepository;
    private final FollowRepository followRepository;
    private final JwtKeyHelper jwtKeyHelper;

    @Transactional(readOnly = true)
    public Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email);
    }
    
    @Transactional(readOnly = true)
    public MemberDto.Member findMemberDtoByEmail(String email) {
        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new MemberNotFoundException();
        }
        return memberMapper.ofMember(member);
    }

    @Transactional(readOnly = true)
    public MemberDto.Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .map(memberMapper::ofMember)
                .orElseThrow(MemberNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<MemberDto.Member> findMembersByIds(List<Long> ids) {
        return memberRepository.findByIdIn(ids).stream()
                .map(memberMapper::ofMember)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "members:memberByToken", key="#token")
    public MemberDto.Member findMemberByToken(String token) {
        JwtDto.UserDetails userDetails = jwtKeyHelper.validateAndParseToken(token);
        return memberRepository.findById(userDetails.getId())
                .map(memberMapper::ofMember)
                .orElseThrow(MemberNotFoundException::new);
    }

    public void checkAndRefreshHeader(String token, HttpServletResponse response) {
        jwtKeyHelper.checkAndRefreshHeader(token, response);
    }

    public MemberDto.MemberProfile findMemberProfileByToken(String token) {
        JwtDto.UserDetails userDetails = jwtKeyHelper.validateAndParseToken(token);

        Member member = memberRepository
                .findById(userDetails.getId())
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


    // TODO token 검증 로직 필요 (AOP 필요하군 ㅋㅋ 아이고..)
    public MemberDto.MemberProfile findAnotherMemberProfile(Long memberId, String token, HttpServletResponse response) {
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
}
